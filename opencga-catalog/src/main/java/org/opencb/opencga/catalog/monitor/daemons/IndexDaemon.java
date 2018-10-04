/*
 * Copyright 2015-2017 OpenCB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opencb.opencga.catalog.monitor.daemons;

import org.opencb.commons.datastore.core.ObjectMap;
import org.opencb.commons.datastore.core.Query;
import org.opencb.commons.datastore.core.QueryOptions;
import org.opencb.commons.datastore.core.QueryResult;
import org.opencb.opencga.catalog.db.api.JobDBAdaptor;
import org.opencb.opencga.catalog.exceptions.CatalogDBException;
import org.opencb.opencga.catalog.exceptions.CatalogException;
import org.opencb.opencga.catalog.exceptions.CatalogIOException;
import org.opencb.opencga.catalog.io.CatalogIOManager;
import org.opencb.opencga.catalog.managers.CatalogManager;
import org.opencb.opencga.catalog.monitor.ExecutionOutputRecorder;
import org.opencb.opencga.catalog.monitor.executors.AbstractExecutor;
import org.opencb.opencga.core.common.TimeUtils;
import org.opencb.opencga.core.common.UriUtils;
import org.opencb.opencga.core.models.Job;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by imedina on 18/08/16.
 */
public class IndexDaemon extends MonitorParentDaemon {

    public static final String INDEX_TYPE = "INDEX_TYPE";
    public static final String ALIGNMENT_TYPE = "ALIGNMENT";
    public static final String VARIANT_TYPE = "VARIANT";

    private static final Query RUNNING_JOBS_QUERY = new Query()
            .append(JobDBAdaptor.QueryParams.STATUS_NAME.key(), Job.JobStatus.RUNNING)
            .append(JobDBAdaptor.QueryParams.TYPE.key(), Job.Type.INDEX);
    private static final Query QUEUED_JOBS_QUERY = new Query()
            .append(JobDBAdaptor.QueryParams.STATUS_NAME.key(), Job.JobStatus.QUEUED)
            .append(JobDBAdaptor.QueryParams.TYPE.key(), Job.Type.INDEX);
    private static final Query PREPARED_JOBS_QUERY = new Query()
            .append(JobDBAdaptor.QueryParams.STATUS_NAME.key(), Job.JobStatus.PREPARED)
            .append(JobDBAdaptor.QueryParams.TYPE.key(), Job.Type.INDEX);

    // Sort jobs by creation date
    private static final QueryOptions QUERY_OPTIONS = new QueryOptions()
            .append(QueryOptions.SORT, JobDBAdaptor.QueryParams.CREATION_DATE.key())
            .append(QueryOptions.ORDER, QueryOptions.ASCENDING);

    // Sort jobs by creation date. Limit to 1 result
    private static final QueryOptions QUERY_OPTIONS_LIMIT_1 = new QueryOptions(QUERY_OPTIONS)
            .append(QueryOptions.LIMIT, 1);

    private CatalogIOManager catalogIOManager;
    private JobDBAdaptor jobDBAdaptor;

    private String binHome;
    private Path tempJobFolder;
//    private VariantIndexOutputRecorder variantIndexOutputRecorder;

    public IndexDaemon(int interval, String sessionId, CatalogManager catalogManager, String appHome)
            throws URISyntaxException, CatalogIOException, CatalogDBException {
        super(interval, sessionId, catalogManager);
        this.binHome = appHome + "/bin/";
        URI uri = UriUtils.createUri(catalogManager.getConfiguration().getTempJobsDir());
        this.tempJobFolder = Paths.get(uri.getPath());
        this.catalogIOManager = catalogManager.getCatalogIOManagerFactory().get("file");
        this.jobDBAdaptor = dbAdaptorFactory.getCatalogJobDBAdaptor();
//        this.variantIndexOutputRecorder = new VariantIndexOutputRecorder(catalogManager, catalogIOManager, sessionId);
    }

    @Override
    public void run() {

        int maxConcurrentIndexJobs = 1; // TODO: Read from configuration?

        while (!exit) {
            try {
                try {
                    Thread.sleep(interval);
                } catch (InterruptedException e) {
                    // Break loop
                    exit = true;
                    break;
                }
                logger.info("----- INDEX DAEMON -----", TimeUtils.getTimeMillis());

            /*
            RUNNING JOBS
             */
                try {
                    QueryResult<Job> runningJobs = jobDBAdaptor.get(RUNNING_JOBS_QUERY, QUERY_OPTIONS);
                    logger.debug("Checking running jobs. {} running jobs found", runningJobs.getNumResults());
                    for (Job job : runningJobs.getResult()) {
                        checkRunningJob(job);
                    }
                } catch (CatalogException e) {
                    logger.warn("Cannot obtain running jobs", e);
                }

            /*
            QUEUED JOBS
             */
                try {
                    QueryResult<Job> queuedJobs = jobDBAdaptor.get(QUEUED_JOBS_QUERY, QUERY_OPTIONS);
                    logger.debug("Checking queued jobs. {} queued jobs found", queuedJobs.getNumResults());
                    for (Job job : queuedJobs.getResult()) {
                        checkQueuedJob(job, tempJobFolder, catalogIOManager);
                    }
                } catch (CatalogException e) {
                    logger.warn("Cannot obtain queued jobs", e);
                }

            /*
            PREPARED JOBS
             */
                try {
                    QueryResult<Job> preparedJobs = jobDBAdaptor.get(PREPARED_JOBS_QUERY, QUERY_OPTIONS_LIMIT_1);
                    if (preparedJobs != null && preparedJobs.getNumResults() > 0) {
                        if (getRunningOrQueuedJobs() < maxConcurrentIndexJobs) {
                            queuePreparedIndex(preparedJobs.first());
                        } else {
                            logger.debug("Too many jobs indexing now, waiting for indexing new jobs");
                        }
                    }
                } catch (CatalogException e) {
                    logger.warn("Cannot obtain prepared jobs", e);
                }
            } catch (RuntimeException e) {
                logger.warn("Catch unexpected exception in IndexDaemon.", e);
                // TODO: Handle exceptions. Continue or shutdown the daemon?
                logger.info("Continue...");
            } catch (Error e) {
                logger.error("Catch error in IndexDaemon.", e);
                throw e;
            }
        }
    }

    private void checkRunningJob(Job job) throws CatalogIOException {
        Path tmpOutdirPath = getJobTemporaryFolder(job.getUid(), tempJobFolder);
        Job.JobStatus jobStatus;

        ExecutionOutputRecorder outputRecorder = new ExecutionOutputRecorder(catalogManager, this.sessionId);
        if (!tmpOutdirPath.toFile().exists()) {
            jobStatus = new Job.JobStatus(Job.JobStatus.ERROR, "Temporal output directory not found");
            try {
                logger.info("Updating job {} from {} to {}", job.getUid(), Job.JobStatus.RUNNING, jobStatus.getName());
                outputRecorder.updateJobStatus(job, jobStatus);
            } catch (CatalogException e) {
                logger.warn("Could not update job {} to status error", job.getUid(), e);
            } finally {
                closeSessionId(job);
            }
        } else {
            String status = executorManager.status(tmpOutdirPath, job);
            if (!status.equalsIgnoreCase(Job.JobStatus.UNKNOWN) && !status.equalsIgnoreCase(Job.JobStatus.RUNNING)) {
                ObjectMap parameters = new ObjectMap(JobDBAdaptor.QueryParams.END_TIME.key(), System.currentTimeMillis());
//                variantIndexOutputRecorder.registerStorageETLResults(job, tmpOutdirPath);
                logger.info("Updating job {} from {} to {}", job.getUid(), Job.JobStatus.RUNNING, status);
                try {
                    if (ALIGNMENT_TYPE.equals(String.valueOf(job.getAttributes().get(INDEX_TYPE)))) {
                        String userToken = catalogManager.getUserManager().getSystemTokenForUser(job.getUserId(), sessionId);
                        outputRecorder.recordJobOutput(job, tmpOutdirPath, userToken);
                        outputRecorder.updateJobStatus(job, new Job.JobStatus(status));
                        cleanPrivateJobInformation(job);
                    } else {
                        // Variant
                        outputRecorder.updateJobStatus(job, new Job.JobStatus(status));
                        if (!status.equals(Job.JobStatus.ERROR)) {
                            logger.info("Removing temporal directory.");
                            this.catalogIOManager.deleteDirectory(UriUtils.createUri(tmpOutdirPath.toString()));
                        } else {
                            logger.info("Keeping temporal directory from an error job : {}", tmpOutdirPath);
                        }
                    }
                } catch (CatalogException | URISyntaxException e) {
                    logger.error("Error removing temporal directory", e);
                } catch (IOException e) {
                    logger.error("Error recording files generated to Catalog", e);
                } finally {
                    closeSessionId(job);
                }

                try {
                    jobDBAdaptor.update(job.getUid(), parameters, QueryOptions.empty());
                } catch (CatalogException e) {
                    logger.error("Error updating job {} with {}", job.getUid(), parameters.toJson(), e);
                }
            }


//            Path jobStatusFile = tmpOutdirPath.resolve(JOB_STATUS_FILE);
//            if (jobStatusFile.toFile().exists()) {
//                try {
//                    jobStatus = objectReader.readValue(jobStatusFile.toFile());
//                } catch (IOException e) {
//                    logger.warn("Could not read job status file.");
//                    return;
//                    // TODO: Add a maximum number of attempts....
//                }
//                if (jobStatus != null && !jobStatus.getName().equalsIgnoreCase(Job.JobStatus.RUNNING)) {
//                    String sessionId = (String) job.getResourceManagerAttributes().get("sessionId");
//                    ExecutionOutputRecorder outputRecorder = new ExecutionOutputRecorder(catalogManager, sessionId);
//                    try {
//                        outputRecorder.recordJobOutputAndPostProcess(job, jobStatus);
//
//                    } catch (CatalogException | IOException e) {
//                        logger.error(e.getMessage());
//                    }
//                }
//            } else {
//                // TODO: Call the executor status
//                logger.debug("Call executor status not yet implemented.");
////                    executorManager.status(job).equalsIgnoreCase()
//            }
        }
    }

    private void queuePreparedIndex(Job job) {
        // Create the temporal output directory.
        Path path = getJobTemporaryFolder(job.getUid(), tempJobFolder);
        try {
            catalogIOManager.createDirectory(path.toUri());
        } catch (CatalogIOException e) {
            logger.warn("Could not create the temporal output directory " + path + " to run the job", e);
            return;
            // TODO: Maximum attemps ... -> Error !
        }

        // Define where the stdout and stderr will be stored
        String stderr = path.resolve(job.getName() + '_' + job.getUid() + ".err").toString();
        String stdout = path.resolve(job.getName() + '_' + job.getUid() + ".out").toString();

        // Obtain a new session id for the user so we can guarantee the session will be open during execution.
        String userId = job.getUserId();
        String userSessionId = null;
        try {
            userSessionId = catalogManager.getUserManager().getSystemTokenForUser(userId, sessionId);
        } catch (CatalogException e) {
            logger.warn("Could not obtain a new session id for user {}. ", userId, e);
        }

        // TODO: This command line could be created outside this class
        // Build the command line.
        StringBuilder commandLine = new StringBuilder(binHome).append(job.getExecutable());

        // we assume job.output equals params.outdir
        job.getParams().put("outdir", path.toString());

        if (job.getAttributes().get(INDEX_TYPE).toString().equalsIgnoreCase(VARIANT_TYPE)) {
            try {
                job.getParams().put("path", dbAdaptorFactory.getCatalogFileDBAdaptor().get(job.getOutDir().getUid(),
                        QueryOptions.empty()).first().getId());
            } catch (CatalogDBException e) {
                e.printStackTrace();
            }

            commandLine.append(" variant index");
            Set<String> knownParams = new HashSet<>(Arrays.asList(
                    "aggregated", "aggregation-mapping-file", "annotate", "annotator", "bgzip", "calculate-stats",
                    "exclude-genotypes", "file", "gvcf", "h", "help", "include-extra-fields", "load", "log-file",
                    "L", "log-level", "merge", "o", "outdir", "overwrite-annotations", "path", "queue", "s", "study",
                    "transform", "transformed-files", "resume", "load-split-data"));
            buildCommandLine(job.getParams(), commandLine, knownParams);
        } else {
            commandLine.append(" alignment index");
            for (Map.Entry<String, String> param : job.getParams().entrySet()) {
                commandLine.append(' ');
                commandLine.append("--").append(param.getKey());
                if (!param.getValue().equalsIgnoreCase("true")) {
                    commandLine.append(" ").append(param.getValue());
                }
            }
        }

        logger.info("Updating job CLI '{}' from '{}' to '{}'", commandLine.toString(), Job.JobStatus.PREPARED, Job.JobStatus.QUEUED);

        try {
            setNewStatus(job.getUid(), Job.JobStatus.QUEUED, "The job is in the queue waiting to be executed");
//            Job.JobStatus jobStatus = new Job.JobStatus(Job.JobStatus.QUEUED, "The job is in the queue waiting to be executed");
//            updateObjectMap.put(JobDBAdaptor.QueryParams.STATUS.key(), jobStatus);
            ObjectMap updateObjectMap = new ObjectMap();
            updateObjectMap.put(JobDBAdaptor.QueryParams.COMMAND_LINE.key(), commandLine.toString());
//            job.getAttributes().put("sessionId", userSessionId);

            updateObjectMap.put(JobDBAdaptor.QueryParams.START_TIME.key(), System.currentTimeMillis());
            updateObjectMap.put(JobDBAdaptor.QueryParams.ATTRIBUTES.key(), job.getAttributes());

            job.getResourceManagerAttributes().put(AbstractExecutor.STDOUT, stdout);
            job.getResourceManagerAttributes().put(AbstractExecutor.STDERR, stderr);
            job.getResourceManagerAttributes().put(AbstractExecutor.OUTDIR, path.toString());
            updateObjectMap.put(JobDBAdaptor.QueryParams.RESOURCE_MANAGER_ATTRIBUTES.key(), job.getResourceManagerAttributes());

            QueryResult<Job> update = jobDBAdaptor.update(job.getUid(), updateObjectMap, QueryOptions.empty());
            if (update.getNumResults() == 1) {
                job = update.first();
                executeJob(job, userSessionId);
            } else {
                logger.error("Could not update nor run job {}" + job.getUid());
            }
        } catch (CatalogException e) {
            logger.error("Could not update job {}.", job.getUid(), e);
        }

    }

    private long getRunningOrQueuedJobs() throws CatalogException {
        Query runningJobsQuery = new Query()
                .append(JobDBAdaptor.QueryParams.STATUS_NAME.key(), Arrays.asList(Job.JobStatus.RUNNING, Job.JobStatus.QUEUED))
                .append(JobDBAdaptor.QueryParams.TYPE.key(), Job.Type.INDEX);
        return jobDBAdaptor.get(runningJobsQuery, QueryOptions.empty()).getNumTotalResults();
    }

    void closeSessionId(Job job) {

        // Remove the session id from the job attributes
        job.getAttributes().remove("sessionId");
        ObjectMap params = new ObjectMap(JobDBAdaptor.QueryParams.ATTRIBUTES.key(), job.getAttributes());
        try {
            jobDBAdaptor.update(job.getUid(), params, QueryOptions.empty());
        } catch (CatalogException e) {
            logger.error("Could not remove session id from attributes of job {}. ", job.getUid(), e);
        }
    }
}
