package org.opencb.opencga.catalog.stats.solr.converters;

import org.apache.commons.lang3.NotImplementedException;
import org.opencb.commons.datastore.core.ComplexTypeConverter;
import org.opencb.commons.datastore.core.QueryParam;
import org.opencb.opencga.catalog.exceptions.CatalogDBException;
import org.opencb.opencga.catalog.stats.solr.FileSolrModel;
import org.opencb.opencga.catalog.utils.AnnotationUtils;
import org.opencb.opencga.core.common.TimeUtils;
import org.opencb.opencga.core.models.AnnotationSet;
import org.opencb.opencga.core.models.File;
import org.opencb.opencga.core.models.Study;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by wasim on 04/07/18.
 */
public class CatalogFileToSolrFileConverter implements ComplexTypeConverter<File, FileSolrModel> {

    private Study study;
    private Map<String, Map<String, QueryParam.Type>> variableMap;

    protected static Logger logger = LoggerFactory.getLogger(CatalogFileToSolrFileConverter.class);

    public CatalogFileToSolrFileConverter(Study study) {
        this.study = study;
        this.variableMap = new HashMap<>();
        if (this.study.getVariableSets() != null) {
            this.study.getVariableSets().forEach(variableSet -> {
                try {
                    this.variableMap.put(variableSet.getId(), AnnotationUtils.getVariableMap(variableSet));
                } catch (CatalogDBException e) {
                    logger.warn("Could not parse variableSet {}: {}", variableSet.getId(), e.getMessage(), e);
                }
            });
        }
    }

    @Override
    public File convertToDataModelType(FileSolrModel object) {
        throw new NotImplementedException("Operation not supported");
    }

    @Override
    public FileSolrModel convertToStorageType(File file) {
        FileSolrModel fileSolrModel = new FileSolrModel();

        fileSolrModel.setUid(file.getUid());
        fileSolrModel.setName(file.getName());
        fileSolrModel.setStudyId(study.getFqn().replace(":", "__"));
        fileSolrModel.setType(file.getType().name());
        if (file.getFormat() != null) {
            fileSolrModel.setFormat(file.getFormat().name());
        }
        if (file.getBioformat() != null) {
            fileSolrModel.setBioformat(file.getBioformat().name());
        }
        fileSolrModel.setRelease(file.getRelease());

        Date date = TimeUtils.toDate(file.getCreationDate());
        LocalDate localDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

        fileSolrModel.setCreationYear(localDate.getYear());
        fileSolrModel.setCreationMonth(localDate.getMonth().toString());
        fileSolrModel.setCreationDay(localDate.getDayOfMonth());
        fileSolrModel.setCreationDayOfWeek(localDate.getDayOfWeek().toString());
        fileSolrModel.setStatus(file.getStatus().getName());

        fileSolrModel.setStatus(file.getStatus().getName());
        fileSolrModel.setExternal(file.isExternal());
        fileSolrModel.setSize(file.getSize());
        if (file.getSoftware() != null) {
            fileSolrModel.setSoftware(file.getSoftware().getName());
        }
//        fileSolrModel.setExperiment(file.getExperiment().getName());

        fileSolrModel.setNumSamples(file.getSamples() != null ? file.getSamples().size() : 0);
        fileSolrModel.setNumRelatedFiles(file.getRelatedFiles() != null ? file.getRelatedFiles().size() : 0);

        fileSolrModel.setAnnotations(SolrConverterUtil.populateAnnotations(variableMap, file.getAnnotationSets()));
        if (file.getAnnotationSets() != null) {
            fileSolrModel.setAnnotationSets(file.getAnnotationSets().stream().map(AnnotationSet::getId).collect(Collectors.toList()));
        } else {
            fileSolrModel.setAnnotationSets(Collections.emptyList());
        }

        // Extract the permissions
        Map<String, Set<String>> fileAcl =
                SolrConverterUtil.parseInternalOpenCGAAcls((List<Map<String, Object>>) file.getAttributes().get("OPENCGA_ACL"));
        List<String> effectivePermissions =
                SolrConverterUtil.getEffectivePermissions((Map<String, Set<String>>) study.getAttributes().get("OPENCGA_ACL"), fileAcl,
                        "FILE");
        fileSolrModel.setAcl(effectivePermissions);

        return fileSolrModel;
    }
}
