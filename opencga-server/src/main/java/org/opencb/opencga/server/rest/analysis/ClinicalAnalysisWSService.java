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

package org.opencb.opencga.server.rest.analysis;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang3.BooleanUtils;
import org.opencb.commons.datastore.core.QueryResponse;
// import org.opencb.opencga.analysis.ClinicalInterpretationAnalysis;
import org.opencb.opencga.core.config.Configuration;
import org.opencb.opencga.core.exception.VersionException;
// import org.opencb.opencga.core.models.clinical.Interpretation;
import org.opencb.opencga.server.rest.OpenCGAWSServer;
import org.opencb.opencga.storage.core.config.StorageConfiguration;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@Path("/{apiVersion}/analysis/clinical")
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "Analysis - Clinical Interpretation", position = 4, description = "Methods for working with Clinical Analysis")
public class ClinicalAnalysisWSService extends AnalysisWSService {

    public ClinicalAnalysisWSService(@Context UriInfo uriInfo, @Context HttpServletRequest httpServletRequest, @Context HttpHeaders httpHeaders)
            throws IOException, VersionException {
        this(uriInfo.getPathParameters().getFirst("apiVersion"), uriInfo, httpServletRequest,httpHeaders);
    }

    public ClinicalAnalysisWSService(String apiVersion, @Context UriInfo uriInfo, @Context HttpServletRequest httpServletRequest, @Context HttpHeaders httpHeaders)
            throws IOException, VersionException {
        super(apiVersion, uriInfo, httpServletRequest, httpHeaders);

        // clinicalInterpretationManager = new ClinicalInterpretationManager(catalogManager, storageEngineFactory);
    }

    @GET
    @Path("/index")
    @ApiOperation(value = "TEAM interpretation analysis", position = 14, response = QueryResponse.class)
    public Response index(@ApiParam(value = "Comma separated list of interpretation IDs") @QueryParam(value = "interpretationId") String interpretationId,
                          @ApiParam(value = "Clinical analysis ID") @QueryParam("clinicalAnalysisId") String clinicalAnalysisId,
                          @ApiParam(value = "Study [[user@]project:]study where study and project can be either the id or alias") @QueryParam("study") String study) {
        return Response.ok().build();
    }

    @GET
    @Path("/query")
    @ApiOperation(value = "TEAM interpretation analysis", position = 14, response = QueryResponse.class)
    public Response query(@ApiParam(value = "Comma separated list of interpretation IDs") @QueryParam(value = "interpretationId") String interpretationId,
                          @ApiParam(value = "Clinical analysis ID") @QueryParam("clinicalAnalysisId") String clinicalAnalysisId,
                          @ApiParam(value = "Study [[user@]project:]study where study and project can be either the id or alias") @QueryParam("study") String study) {
        return Response.ok().build();
    }

    @GET
    @Path("/interpretation")
    @ApiOperation(value = "Clinical interpretation analysis", position = 14, response = QueryResponse.class)
    // TODO: remove, as we need a filter nomore
//    @ApiImplicitParams({
//            // Variant filters
//            @ApiImplicitParam(name = "id", value = ID_DESCR, dataType = "string", paramType = "query"),
//            @ApiImplicitParam(name = "region", value = REGION_DESCR, dataType = "string", paramType = "query"),
//            @ApiImplicitParam(name = "type", value = TYPE_DESCR, dataType = "string", paramType = "query"),
//            @ApiImplicitParam(name = "reference", value = REFERENCE_DESCR, dataType = "string", paramType = "query"),
//            @ApiImplicitParam(name = "alternate", value = ALTERNATE_DESCR, dataType = "string", paramType = "query"),
//
//            // Study filters
//            @ApiImplicitParam(name = "project", value = VariantCatalogQueryUtils.PROJECT_DESC, dataType = "string", paramType = "query"),
//            @ApiImplicitParam(name = "study", value = STUDY_DESCR, dataType = "string", paramType = "query"),
//            @ApiImplicitParam(name = "file", value = FILE_DESCR, dataType = "string", paramType = "query"),
//            @ApiImplicitParam(name = "filter", value = FILTER_DESCR, dataType = "string", paramType = "query"),
//            @ApiImplicitParam(name = "qual", value = QUAL_DESCR, dataType = "string", paramType = "query"),
//
//            @ApiImplicitParam(name = "sample", value = SAMPLE_DESCR, dataType = "string", paramType = "query"),
//            @ApiImplicitParam(name = "genotype", value = GENOTYPE_DESCR, dataType = "string", paramType = "query"),
//            @ApiImplicitParam(name = "sampleAnnotation", value = VariantCatalogQueryUtils.SAMPLE_ANNOTATION_DESC, dataType = "string", paramType = "query"),
//            @ApiImplicitParam(name = "sampleMetadata", value = SAMPLE_METADATA_DESCR, dataType = "boolean", paramType = "query"),
//            @ApiImplicitParam(name = "unknownGenotype", value = UNKNOWN_GENOTYPE_DESCR, dataType = "string", paramType = "query"),
//
//            @ApiImplicitParam(name = "cohort", value = COHORT_DESCR, dataType = "string", paramType = "query"),
//            @ApiImplicitParam(name = "maf", value = STATS_MAF_DESCR, dataType = "string", paramType = "query"),
//            @ApiImplicitParam(name = "mgf", value = STATS_MGF_DESCR, dataType = "string", paramType = "query"),
//            @ApiImplicitParam(name = "missingAlleles", value = MISSING_ALLELES_DESCR, dataType = "string", paramType = "query"),
//            @ApiImplicitParam(name = "missingGenotypes", value = MISSING_GENOTYPES_DESCR, dataType = "string", paramType = "query"),
//
//            // Annotation filters
//            @ApiImplicitParam(name = "annotationExists", value = ANNOT_EXISTS_DESCR, dataType = "boolean", paramType = "query"),
//            @ApiImplicitParam(name = "gene", value = GENE_DESCR, dataType = "string", paramType = "query"),
//            @ApiImplicitParam(name = "ct", value = ANNOT_CONSEQUENCE_TYPE_DESCR, dataType = "string", paramType = "query"),
//            @ApiImplicitParam(name = "xref", value = ANNOT_XREF_DESCR, dataType = "string", paramType = "query"),
//            @ApiImplicitParam(name = "biotype", value = ANNOT_BIOTYPE_DESCR, dataType = "string", paramType = "query"),
//            @ApiImplicitParam(name = "proteinSubstitution", value = ANNOT_PROTEIN_SUBSTITUTION_DESCR, dataType = "string", paramType = "query"),
//            @ApiImplicitParam(name = "conservation", value = ANNOT_CONSERVATION_DESCR, dataType = "string", paramType = "query"),
//            @ApiImplicitParam(name = "populationFrequencyAlt", value = ANNOT_POPULATION_ALTERNATE_FREQUENCY_DESCR, dataType = "string", paramType = "query"),
//            @ApiImplicitParam(name = "populationFrequencyRef", value = ANNOT_POPULATION_REFERENCE_FREQUENCY_DESCR, dataType = "string", paramType = "query"),
//            @ApiImplicitParam(name = "populationFrequencyMaf", value = ANNOT_POPULATION_MINOR_ALLELE_FREQUENCY_DESCR, dataType = "string", paramType = "query"),
//            @ApiImplicitParam(name = "transcriptionFlag", value = ANNOT_TRANSCRIPTION_FLAG_DESCR, dataType = "string", paramType = "query"),
//            @ApiImplicitParam(name = "geneTraitId", value = ANNOT_GENE_TRAIT_ID_DESCR, dataType = "string", paramType = "query"),
//            @ApiImplicitParam(name = "go", value = ANNOT_GO_DESCR, dataType = "string", paramType = "query"),
//            @ApiImplicitParam(name = "expression", value = ANNOT_EXPRESSION_DESCR, dataType = "string", paramType = "query"),
//            @ApiImplicitParam(name = "proteinKeyword", value = ANNOT_PROTEIN_KEYWORD_DESCR, dataType = "string", paramType = "query"),
//            @ApiImplicitParam(name = "drug", value = ANNOT_DRUG_DESCR, dataType = "string", paramType = "query"),
//            @ApiImplicitParam(name = "functionalScore", value = ANNOT_FUNCTIONAL_SCORE_DESCR, dataType = "string", paramType = "query"),
//            @ApiImplicitParam(name = "customAnnotation", value = CUSTOM_ANNOTATION_DESCR, dataType = "string", paramType = "query"),
//
//            // WARN: Only available in Solr
//            @ApiImplicitParam(name = "trait", value = ANNOT_TRAIT_DESCR, dataType = "string", paramType = "query"),
//    })
    public Response interpretation(
            @ApiParam(value = "Study ID") @QueryParam("study") String studyStr,
            @ApiParam(value = "Panel ID", required=true) @QueryParam("panelId") String panelId,
            @ApiParam(value = "Panel version") @QueryParam("panelVersion") String panelVersion,
            @ApiParam(value = "Sample ID", required=true) @QueryParam("sampleId") String sampleId,
            @ApiParam(value = "Whether to look for Variants of Uncertain Significance") @QueryParam("searchForVUS") Boolean searchForVUS,
            @ApiParam(value = "Whether to look for potential unexpected findings") @QueryParam("searchForUF") Boolean searchForUF,
            @ApiParam(value = "ID of the interpretation") @QueryParam("interpretationId") String interpretationId,
            @ApiParam(value = "Name of the interpretation") @QueryParam("interpretationName") String interpretationName
            // @ApiParam(value = "Clinical Analysis Id") @QueryParam("clinicalAnalysisId") String clinicalAnalysisId,
            // @ApiParam(value = "Disease (HPO term)") @QueryParam("disease") String disease,
            // @ApiParam(value = "Family ID") @QueryParam("familyId") String familyId,
            // @ApiParam(value = "Comma separated list of subject IDs") @QueryParam("subjectIds") List<String> subjectIds,
            // @ApiParam(value = "Clinical analysis type, e.g. DUO, TRIO, ...") @QueryParam("type") String type,
            // @ApiParam(value = "Save interpretation in Catalog") @QueryParam("save") Boolean save,
    ) {
    	return Response.ok().build();
    	
        /* try {
            Configuration configuration = OpenCGAWSServer.configuration;
            StorageConfiguration storageConfiguration = OpenCGAWSServer.storageConfiguration;
            ClinicalInterpretationAnalysis analysis = new ClinicalInterpretationAnalysis(
                    configuration, storageConfiguration,
                    studyStr, panelId, panelVersion, sampleId, interpretationId, interpretationName,
                    BooleanUtils.isTrue(searchForVUS), BooleanUtils.isTrue(searchForUF), sessionId);
            analysis.execute();
            Interpretation interpretation = analysis.getInterpretation();
            if (null != interpretation) {
                return createOkResponse(interpretation);
        		
            } else {
                return createOkResponse("no results were generated");
            }
        } catch (Exception exc) {
            return createErrorResponse(exc);
        }*/
    }

    @GET
    @Path("/tiering")
    @ApiOperation(value = "GEL Tiering interpretation analysis (PENDING)", position = 14, response = QueryResponse.class)
    public Response tiering(@ApiParam(value = "(DEPRECATED) Comma separated list of file ids (files or directories)", hidden = true)
                            @QueryParam(value = "fileId") String fileIdStrOld,
                            @ApiParam(value = "Comma separated list of file ids (files or directories)", required = true)
                            @QueryParam(value = "file") String fileIdStr,
                            // Study id is not ingested by the analysis index command line. No longer needed.
                            @ApiParam(value = "(DEPRECATED) Study id", hidden = true) @QueryParam("studyId") String studyStrOld,
                            @ApiParam(value = "Study [[user@]project:]study where study and project can be either the id or alias")
                            @QueryParam("study") String studyStr) {

        return Response.ok().build();
    }

}
