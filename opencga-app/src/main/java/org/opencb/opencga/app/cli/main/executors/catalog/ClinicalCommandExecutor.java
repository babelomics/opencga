package org.opencb.opencga.app.cli.main.executors.catalog;

import org.opencb.commons.datastore.core.ObjectMap;
import org.opencb.commons.datastore.core.Query;
import org.opencb.commons.datastore.core.QueryOptions;
import org.opencb.commons.datastore.core.QueryResponse;
import org.opencb.opencga.app.cli.main.executors.OpencgaCommandExecutor;
import org.opencb.opencga.app.cli.main.executors.catalog.commons.AclCommandExecutor;
import org.opencb.opencga.app.cli.main.options.ClinicalCommandOptions;
import org.opencb.opencga.app.cli.main.options.commons.AclCommandOptions;
import org.opencb.opencga.catalog.db.api.ClinicalAnalysisDBAdaptor;
import org.opencb.opencga.catalog.exceptions.CatalogException;
import org.opencb.opencga.core.models.ClinicalAnalysis;
import org.opencb.opencga.core.models.acls.permissions.ClinicalAnalysisAclEntry;

import java.io.IOException;

public class ClinicalCommandExecutor extends OpencgaCommandExecutor {

    private ClinicalCommandOptions clinicalCommandOptions;
    private AclCommandExecutor<ClinicalAnalysis, ClinicalAnalysisAclEntry> aclCommandExecutor;

    public ClinicalCommandExecutor(ClinicalCommandOptions clinicalCommandOptions) {
        super(clinicalCommandOptions.commonCommandOptions);
        this.clinicalCommandOptions = clinicalCommandOptions;
        this.aclCommandExecutor = new AclCommandExecutor<>();
    }

    @Override
    public void execute() throws Exception {
        logger.debug("Executing clinical analysis command line");

        String subCommandString = getParsedSubCommand(clinicalCommandOptions.jCommander);
        QueryResponse queryResponse = null;
        switch (subCommandString) {
            case "info":
                queryResponse = info();
                break;
            case "search":
                queryResponse = search();
                break;
            case "group-by":
                queryResponse = groupBy();
                break;
            case "acl":
                queryResponse = aclCommandExecutor.acls(clinicalCommandOptions.aclsCommandOptions,
                        openCGAClient.getClinicalAnalysisClient());
                break;
            case "acl-update":
                queryResponse = updateAcl();
                break;
            default:
                logger.error("Subcommand not valid");
                break;
        }

        createOutput(queryResponse);
    }

    private QueryResponse<ClinicalAnalysis> groupBy() throws IOException {
        logger.debug("Clinical analysis groupby");

        ClinicalCommandOptions.GroupByCommandOptions commandOptions = clinicalCommandOptions.groupByCommandOptions;

        ObjectMap params = new ObjectMap();
        params.putIfNotEmpty(ClinicalAnalysisDBAdaptor.QueryParams.STUDY.key(), resolveStudy(commandOptions.study));
        params.putIfNotNull(ClinicalAnalysisDBAdaptor.QueryParams.TYPE.key(), commandOptions.type);
        params.putIfNotNull(ClinicalAnalysisDBAdaptor.QueryParams.STATUS.key(), commandOptions.status);
        params.putIfNotNull(ClinicalAnalysisDBAdaptor.QueryParams.GERMLINE.key(), commandOptions.germline);
        params.putIfNotNull(ClinicalAnalysisDBAdaptor.QueryParams.SOMATIC.key(), commandOptions.somatic);
        params.putIfNotNull(ClinicalAnalysisDBAdaptor.QueryParams.FAMILY.key(), commandOptions.family);
        params.putIfNotNull("subject", commandOptions.subject);
        params.putIfNotEmpty("sample", commandOptions.sample);
        params.putAll(commandOptions.commonOptions.params);

        return openCGAClient.getClinicalAnalysisClient().groupBy(commandOptions.study, commandOptions.fields, params);
    }

    private QueryResponse<ClinicalAnalysis> search() throws IOException {
        logger.debug("Clinical analysis search");

        ClinicalCommandOptions.SearchCommandOptions commandOptions = clinicalCommandOptions.searchCommandOptions;

        Query query = new Query();
        query.putIfNotEmpty(ClinicalAnalysisDBAdaptor.QueryParams.STUDY.key(), resolveStudy(commandOptions.study));
        query.putIfNotNull(ClinicalAnalysisDBAdaptor.QueryParams.TYPE.key(), commandOptions.type);
        query.putIfNotNull(ClinicalAnalysisDBAdaptor.QueryParams.STATUS.key(), commandOptions.status);
        query.putIfNotNull(ClinicalAnalysisDBAdaptor.QueryParams.GERMLINE.key(), commandOptions.germline);
        query.putIfNotNull(ClinicalAnalysisDBAdaptor.QueryParams.SOMATIC.key(), commandOptions.somatic);
        query.putIfNotNull(ClinicalAnalysisDBAdaptor.QueryParams.FAMILY.key(), commandOptions.family);
        query.putIfNotNull("subject", commandOptions.subject);
        query.putIfNotEmpty("sample", commandOptions.sample);
        query.putAll(commandOptions.commonOptions.params);

        if (commandOptions.numericOptions.count) {
            return openCGAClient.getClinicalAnalysisClient().count(query);
        } else {
            QueryOptions queryOptions = new QueryOptions();
            queryOptions.putIfNotEmpty(QueryOptions.INCLUDE, commandOptions.dataModelOptions.include);
            queryOptions.putIfNotEmpty(QueryOptions.EXCLUDE, commandOptions.dataModelOptions.exclude);
            queryOptions.put(QueryOptions.LIMIT, commandOptions.numericOptions.limit);
            queryOptions.put(QueryOptions.SKIP, commandOptions.numericOptions.skip);

            return openCGAClient.getClinicalAnalysisClient().search(query, queryOptions);
        }
    }

    private QueryResponse<ClinicalAnalysis> info() throws CatalogException, IOException {
        logger.debug("Getting clinical analysis information");

        ClinicalCommandOptions.InfoCommandOptions commandOptions = clinicalCommandOptions.infoCommandOptions;

        ObjectMap params = new ObjectMap();
        params.putIfNotEmpty(ClinicalAnalysisDBAdaptor.QueryParams.STUDY.key(), resolveStudy(commandOptions.study));
        params.putIfNotEmpty(QueryOptions.INCLUDE, commandOptions.dataModelOptions.include);
        params.putIfNotEmpty(QueryOptions.EXCLUDE, commandOptions.dataModelOptions.exclude);
        return openCGAClient.getClinicalAnalysisClient().get(commandOptions.clinical, params);
    }

    private QueryResponse<ClinicalAnalysisAclEntry> updateAcl() throws IOException, CatalogException {
        AclCommandOptions.AclsUpdateCommandOptions commandOptions = clinicalCommandOptions.aclsUpdateCommandOptions;

        ObjectMap queryParams = new ObjectMap();
        queryParams.putIfNotNull("study", commandOptions.study);

        ObjectMap bodyParams = new ObjectMap();
        bodyParams.putIfNotNull("permissions", commandOptions.permissions);
        bodyParams.putIfNotNull("action", commandOptions.action);
        bodyParams.putIfNotNull("clinicalAnalysis", extractIdsFromListOrFile(commandOptions.id));

        return openCGAClient.getClinicalAnalysisClient().updateAcl(commandOptions.memberId, queryParams, bodyParams);
    }

}
