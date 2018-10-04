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

package org.opencb.opencga.storage.core.variant.search.solr;

import org.apache.solr.client.solrj.SolrQuery;
import org.junit.Before;
import org.junit.Test;
import org.opencb.commons.datastore.core.Query;
import org.opencb.commons.datastore.core.QueryOptions;
import org.opencb.opencga.storage.core.exceptions.StorageEngineException;
import org.opencb.opencga.storage.core.metadata.StudyConfigurationManager;
import org.opencb.opencga.storage.core.variant.adaptors.VariantField;
import org.opencb.opencga.storage.core.variant.adaptors.VariantQueryException;
import org.opencb.opencga.storage.core.variant.dummy.DummyProjectMetadataAdaptor;
import org.opencb.opencga.storage.core.variant.dummy.DummyStudyConfigurationAdaptor;
import org.opencb.opencga.storage.core.variant.dummy.DummyVariantFileMetadataDBAdaptor;

import static org.junit.Assert.fail;
import static org.opencb.opencga.storage.core.variant.adaptors.VariantQueryParam.*;

/**
 * Created by jtarraga on 03/03/17.
 */
public class SolrQueryParserTest {

    private String flBase = "fl=popFreq_*,geneToSoAcc,traits,caddScaled,genes,stats_*,chromosome,xrefs,start,gerp,type,soAcc,polyphen,sift,siftDesc,caddRaw,biotypes,polyphenDesc,studies,end,id,variantId,phastCons,phylop,id,chromosome,start,end,type";
    private String flDefault1 = flBase + ",fileInfo__*,qual__*,filter__*,sampleFormat__*";

    SolrQueryParser solrQueryParser;

    String studyName = "platinum";
    StudyConfigurationManager scm;

    @Before
    public void init() throws StorageEngineException {
        scm = new StudyConfigurationManager(new DummyProjectMetadataAdaptor(), new DummyStudyConfigurationAdaptor(), new DummyVariantFileMetadataDBAdaptor());
        scm.createStudy(studyName);

        solrQueryParser = new SolrQueryParser(scm);
    }

    @Test
    public void parseXref() {
        QueryOptions queryOptions = new QueryOptions();

        Query query = new Query();
        query.put(ANNOT_XREF.key(), "rs574335987");

        SolrQuery solrQuery = solrQueryParser.parse(query, queryOptions);
        display(query, queryOptions, solrQuery);
        assert((flDefault1 + "&q=*:*&fq=xrefs:\"rs574335987\"").equals(solrQuery.toString()));
    }

    @Test
    public void parseConsequenceTypeSOTerm() {
        QueryOptions queryOptions = new QueryOptions();

        Query query = new Query();
        query.put(ANNOT_CONSEQUENCE_TYPE.key(), "missense_variant");

        SolrQuery solrQuery = solrQueryParser.parse(query, queryOptions);
        display(query, queryOptions, solrQuery);
        assert((flDefault1 + "&q=*:*&fq=soAcc:\"1583\"").equals(solrQuery.toString()));
    }

    @Test
    public void parseConsequenceTypeSOAcc() {
        QueryOptions queryOptions = new QueryOptions();

        Query query = new Query();
        query.put(ANNOT_CONSEQUENCE_TYPE.key(), "SO:0001792,SO:0001619");

        SolrQuery solrQuery = solrQueryParser.parse(query, queryOptions);
        display(query, queryOptions, solrQuery);
        assert((flDefault1 + "&q=*:*&fq=soAcc:\"1792\"+OR+soAcc:\"1619\"").equals(solrQuery.toString()));
    }

    @Test
    public void parseGeneAndConsequenceType() {
        QueryOptions queryOptions = new QueryOptions();

        Query query = new Query();
        query.put(GENE.key(), "WASH7P");
        query.put(ANNOT_CONSEQUENCE_TYPE.key(), "SO:0001792");

        SolrQuery solrQuery = solrQueryParser.parse(query, queryOptions);
        display(query, queryOptions, solrQuery);
        assert((flDefault1 + "&q=*:*&fq=geneToSoAcc:\"WASH7P_1792\"").equals(solrQuery.toString()));
    }

    @Test
    public void parseRegion() {
        QueryOptions queryOptions = new QueryOptions();

        Query query = new Query();
        query.put(REGION.key(), "1:17700");

        SolrQuery solrQuery = solrQueryParser.parse(query, queryOptions);
        display(query, queryOptions, solrQuery);
        assert((flDefault1 + "&q=*:*&fq=(chromosome:\"1\"+AND+start:17700)").equals(solrQuery.toString()));
    }

    @Test
    public void parseType() {
        QueryOptions queryOptions = new QueryOptions();

        Query query = new Query();
        query.put(TYPE.key(), "CNV,SNV");

        SolrQuery solrQuery = solrQueryParser.parse(query, queryOptions);
        display(query, queryOptions, solrQuery);
        assert((flDefault1 + "&q=*:*&fq=(type:\"CNV\"+OR+type:\"SNV\")").equals(solrQuery.toString()));
    }

    @Test
    public void parsePhylop() {
        QueryOptions queryOptions = new QueryOptions();

        Query query = new Query();
        query.put(ANNOT_CONSERVATION.key(), "phylop<-1.0");

        SolrQuery solrQuery = solrQueryParser.parse(query, queryOptions);
        display(query, queryOptions, solrQuery);
        assert((flDefault1 + "&q=*:*&fq=phylop:{-100.0+TO+-1.0}").equals(solrQuery.toString()));
    }

    @Test
    public void parsePhylopAndGene() {
        QueryOptions queryOptions = new QueryOptions();

        Query query = new Query();
        query.put(GENE.key(), "WASH7P");
        query.put(ANNOT_CONSERVATION.key(), "phylop<-1.0");

        SolrQuery solrQuery = solrQueryParser.parse(query, queryOptions);
        display(query, queryOptions, solrQuery);
        assert((flDefault1 + "&q=*:*&fq=xrefs:\"WASH7P\"&fq=phylop:{-100.0+TO+-1.0}").equals(solrQuery.toString()));
    }

    @Test
    public void parsePhylopAndGeneAndPop() {
        QueryOptions queryOptions = new QueryOptions();

        Query query = new Query();
        query.put(GENE.key(), "WASH7P");
        query.put(ANNOT_CONSERVATION.key(), "phylop<-1.0");
        query.put(ANNOT_POPULATION_MINOR_ALLELE_FREQUENCY.key(), "1kG_phase3:ALL>0.1");

        SolrQuery solrQuery = solrQueryParser.parse(query, queryOptions);
        display(query, queryOptions, solrQuery);
        assert((flDefault1 + "&q=*:*&fq=xrefs:\"WASH7P\"&fq=phylop:{-100.0+TO+-1.0}&fq=popFreq__1kG_phase3__ALL:{0.1+TO+*]").equals(solrQuery.toString()));
    }

    @Test
    public void parseSiftScore() {
        QueryOptions queryOptions = new QueryOptions();

        Query query = new Query();
        query.put(ANNOT_PROTEIN_SUBSTITUTION.key(), "sift==tolerated,polyphen==bening");

        SolrQuery solrQuery = solrQueryParser.parse(query, queryOptions);
        display(query, queryOptions, solrQuery);
        assert((flDefault1 + "&q=*:*&fq=(siftDesc:\"tolerated\"+OR+polyphenDesc:\"bening\")").equals(solrQuery.toString()));
    }

    @Test
    public void parsePopMafScore() {
        QueryOptions queryOptions = new QueryOptions();

        Query query = new Query();
        query.put(ANNOT_POPULATION_MINOR_ALLELE_FREQUENCY.key(), "1kG_phase3:YRI<0.01");

        SolrQuery solrQuery = solrQueryParser.parse(query, queryOptions);
        display(query, queryOptions, solrQuery);
        assert((flDefault1 + "&q=*:*&fq=(popFreq__1kG_phase3__YRI:[0+TO+0.01}+OR+(*+-popFreq__1kG_phase3__YRI:*))").equals(solrQuery.toString()));
    }

    @Test
    public void parsePopMafScoreMissing() {
        QueryOptions queryOptions = new QueryOptions();

        Query query = new Query();
        // (* -popFreq__1kG_phase3__YRI:*) OR popFreq_1kG_phase3__YRI:[0.01 TO *]
        query.put(ANNOT_POPULATION_MINOR_ALLELE_FREQUENCY.key(), "1kG_phase3:YRI<<0.01");

        SolrQuery solrQuery = solrQueryParser.parse(query, queryOptions);
        display(query, queryOptions, solrQuery);
        assert((flDefault1 + "&q=*:*&fq=(popFreq__1kG_phase3__YRI:[0+TO+0.01}+OR+(*+-popFreq__1kG_phase3__YRI:*))").equals(solrQuery.toString()));
    }

    @Test
    public void parsePhastCons() {
        QueryOptions queryOptions = new QueryOptions();

        Query query = new Query();
        query.put(ANNOT_CONSERVATION.key(), "phastCons>0.02");

        SolrQuery solrQuery = solrQueryParser.parse(query, queryOptions);
        display(query, queryOptions, solrQuery);
        assert((flDefault1 + "&q=*:*&fq=phastCons:{0.02+TO+*]").equals(solrQuery.toString()));
    }

    @Test
    public void parsePhastConsMissing() {
        QueryOptions queryOptions = new QueryOptions();

        Query query = new Query();
        query.put(ANNOT_CONSERVATION.key(), "phastCons>>0.02");

        SolrQuery solrQuery = solrQueryParser.parse(query, queryOptions);
        display(query, queryOptions, solrQuery);
        assert((flDefault1 + "&q=*:*&fq=(phastCons:{0.02+TO+*]+OR+phastCons:\\-100.0)").equals(solrQuery.toString()));
    }

    @Test
    public void parseSiftMissing() {
        QueryOptions queryOptions = new QueryOptions();

        Query query = new Query();
        //query.put(STUDIES.key(), study);
        query.put(ANNOT_PROTEIN_SUBSTITUTION.key(), "sift<<0.01");

        SolrQuery solrQuery = solrQueryParser.parse(query, queryOptions);
        display(query, queryOptions, solrQuery);
        assert((flDefault1 + "&q=*:*&fq=sift:[-100.0+TO+0.01}").equals(solrQuery.toString()));
    }

    @Test
    public void parseNoPhastCons() {
        QueryOptions queryOptions = new QueryOptions();

        Query query = new Query();
        //query.put(STUDIES.key(), study);
        query.put(ANNOT_CONSERVATION.key(), "phastCons!=0.035999998450279236");

        SolrQuery solrQuery = solrQueryParser.parse(query, queryOptions);
        display(query, queryOptions, solrQuery);
        assert((flDefault1 + "&q=*:*&fq=-phastCons:0.035999998450279236").equals(solrQuery.toString()));
    }

    @Test
    public void parseExactPhastCons() {
        QueryOptions queryOptions = new QueryOptions();

        Query query = new Query();
        query.put(ANNOT_CONSERVATION.key(), "phastCons=0.035999998450279236");

        SolrQuery solrQuery = solrQueryParser.parse(query, queryOptions);
        display(query, queryOptions, solrQuery);
        assert((flDefault1 + "&q=*:*&fq=phastCons:0.035999998450279236").equals(solrQuery.toString()));
    }

    @Test
    public void parseNoPopMaf() {
        QueryOptions queryOptions = new QueryOptions();

        Query query = new Query();
        query.put(ANNOT_POPULATION_MINOR_ALLELE_FREQUENCY.key(), "1kG_phase3:GWD!=0.061946902");

        SolrQuery solrQuery = solrQueryParser.parse(query, queryOptions);
        display(query, queryOptions, solrQuery);
        assert((flDefault1 + "&q=*:*&fq=-popFreq__1kG_phase3__GWD:0.061946902").equals(solrQuery.toString()));
    }

    @Test
    public void parseExactPopMaf() {
        QueryOptions queryOptions = new QueryOptions();

        Query query = new Query();
        query.put(ANNOT_POPULATION_MINOR_ALLELE_FREQUENCY.key(), "1kG_phase3:GWD==0.061946902");

        SolrQuery solrQuery = solrQueryParser.parse(query, queryOptions);
        display(query, queryOptions, solrQuery);
        assert((flDefault1 + "&q=*:*&fq=popFreq__1kG_phase3__GWD:0.061946902").equals(solrQuery.toString()));
    }

    @Test
    public void parseExactSiftDesc() {
        QueryOptions queryOptions = new QueryOptions();

        Query query = new Query();
        query.put(ANNOT_PROTEIN_SUBSTITUTION.key(), "sift==tolerated");

        SolrQuery solrQuery = solrQueryParser.parse(query, queryOptions);
        display(query, queryOptions, solrQuery);
        assert((flDefault1 + "&q=*:*&fq=siftDesc:\"tolerated\"").equals(solrQuery.toString()));
    }

    @Test
    public void parseExactSiftDesc2() {
        QueryOptions queryOptions = new QueryOptions();

        Query query = new Query();
        query.put(ANNOT_PROTEIN_SUBSTITUTION.key(), "sift=tolerated");

        SolrQuery solrQuery = solrQueryParser.parse(query, queryOptions);
        display(query, queryOptions, solrQuery);
        assert((flDefault1 + "&q=*:*&fq=siftDesc:\"tolerated\"").equals(solrQuery.toString()));
    }

    @Test
    public void parseNoExactSiftDesc() {
        QueryOptions queryOptions = new QueryOptions();

        Query query = new Query();
        query.put(ANNOT_PROTEIN_SUBSTITUTION.key(), "sift!=tolerated");

        SolrQuery solrQuery = solrQueryParser.parse(query, queryOptions);
        display(query, queryOptions, solrQuery);
        assert((flDefault1 + "&q=*:*&fq=-siftDesc:\"tolerated\"").equals(solrQuery.toString()));
    }

    @Test
    public void parseExactSift() {
        QueryOptions queryOptions = new QueryOptions();

        Query query = new Query();
        query.put(ANNOT_PROTEIN_SUBSTITUTION.key(), "sift==-0.3");

        SolrQuery solrQuery = solrQueryParser.parse(query, queryOptions);
        display(query, queryOptions, solrQuery);
        assert((flDefault1 + "&q=*:*&fq=sift:\\-0.3").equals(solrQuery.toString()));
    }

    @Test
    public void parseExactSift2() {
        QueryOptions queryOptions = new QueryOptions();

        Query query = new Query();
        query.put(ANNOT_PROTEIN_SUBSTITUTION.key(), "sift=-0.3");

        SolrQuery solrQuery = solrQueryParser.parse(query, queryOptions);
        display(query, queryOptions, solrQuery);
        assert((flDefault1 + "&q=*:*&fq=sift:\\-0.3").equals(solrQuery.toString()));
    }

    @Test
    public void parseNoExactSift() {
        QueryOptions queryOptions = new QueryOptions();

        Query query = new Query();
        query.put(ANNOT_PROTEIN_SUBSTITUTION.key(), "sift!=-0.3");

        SolrQuery solrQuery = solrQueryParser.parse(query, queryOptions);
        display(query, queryOptions, solrQuery);
        assert((flDefault1 + "&q=*:*&fq=-sift:\\-0.3").equals(solrQuery.toString()));
    }

    @Test
    public void parseRegionChromosome() {
        QueryOptions queryOptions = new QueryOptions();

        Query query = new Query();
        query.put(REGION.key(), "1,3");

        SolrQuery solrQuery = solrQueryParser.parse(query, queryOptions);
        display(query, queryOptions, solrQuery);
        assert((flDefault1 + "&q=*:*&fq=(chromosome:\"1\")+OR+(chromosome:\"3\")").equals(solrQuery.toString()));
    }

    @Test
    public void parseRegionChromosomeStart() {
        QueryOptions queryOptions = new QueryOptions();

        Query query = new Query();
        //query.put(STUDIES.key(), study);
        query.put(REGION.key(), "1:66381,1:98769");

        SolrQuery solrQuery = solrQueryParser.parse(query, queryOptions);
        display(query, queryOptions, solrQuery);
        assert((flDefault1 + "&q=*:*&fq=(chromosome:\"1\"+AND+start:66381)+OR+(chromosome:\"1\"+AND+start:98769)").equals(solrQuery.toString()));
    }

    @Test
    public void parseRegionChromosomeStartEnd() {
        QueryOptions queryOptions = new QueryOptions();

        Query query = new Query();
        query.put(REGION.key(), "1:66381-76381,1:98766-117987");

        SolrQuery solrQuery = solrQueryParser.parse(query, queryOptions);
        display(query, queryOptions, solrQuery);
        assert((flDefault1 + "&q=*:*&fq=(chromosome:\"1\"+AND+start:[66381+TO+*]+AND+end:[*+TO+76381])+OR+(chromosome:\"1\"+AND+start:[98766+TO+*]+AND+end:[*+TO+117987])").equals(solrQuery.toString()));
    }

    @Test
    public void parseAnnot() {
        QueryOptions queryOptions = new QueryOptions();

        Query query = new Query();
        query.put(ANNOT_CONSEQUENCE_TYPE.key(), "missense_variant");
        query.put(ANNOT_XREF.key(), "RIPK2,NCF4");

        SolrQuery solrQuery = solrQueryParser.parse(query, queryOptions);
        display(query, queryOptions, solrQuery);
        assert((flDefault1 + "&q=*:*&fq=geneToSoAcc:\"RIPK2_1583\"+OR+geneToSoAcc:\"NCF4_1583\"").equals(solrQuery.toString()));
    }

    @Test
    public void parseAnnotCT1() {
        QueryOptions queryOptions = new QueryOptions();

        Query query = new Query();

        // consequence types and genes
        // no xrefs or regions: genes AND cts
        query.put(ANNOT_CONSEQUENCE_TYPE.key(), "missense_variant,coding_sequence_variant");
        query.put(ANNOT_XREF.key(), "RIPK2,NCF4");

        SolrQuery solrQuery = solrQueryParser.parse(query, queryOptions);
        display(query, queryOptions, solrQuery);
        assert((flDefault1 + "&q=*:*&fq=geneToSoAcc:\"RIPK2_1583\"+OR+geneToSoAcc:\"RIPK2_1580\"+OR+geneToSoAcc:\"NCF4_1583\"+OR+geneToSoAcc:\"NCF4_1580\"").equals(solrQuery.toString()));
    }

    @Test
    public void parseAnnotCT2() {
        QueryOptions queryOptions = new QueryOptions();

        Query query = new Query();

        // consequence types and genes and xrefs/regions
        // otherwise: [((xrefs OR regions) AND cts) OR (genes AND cts)]

        query.put(REGION.key(), "1,2");
        query.put(ANNOT_CONSEQUENCE_TYPE.key(), "missense_variant,coding_sequence_variant");
        query.put(ANNOT_XREF.key(), "RIPK2,NCF4");

        SolrQuery solrQuery = solrQueryParser.parse(query, queryOptions);
        display(query, queryOptions, solrQuery);
        assert((flDefault1 + "&q=*:*&fq=(((chromosome:\"1\")+OR+(chromosome:\"2\"))+AND+(soAcc:\"1583\"+OR+soAcc:\"1580\"))+OR+(geneToSoAcc:\"RIPK2_1583\"+OR+geneToSoAcc:\"RIPK2_1580\"+OR+geneToSoAcc:\"NCF4_1583\"+OR+geneToSoAcc:\"NCF4_1580\")").equals(solrQuery.toString()));
    }

    @Test
    public void parseAnnotCT3() {
        QueryOptions queryOptions = new QueryOptions();

        Query query = new Query();
        //query.put(STUDIES.key(), study);

        // consequence types but no genes: (xrefs OR regions) AND cts
        // in this case, the resulting string will never be null, because there are some consequence types!!
        query.put(REGION.key(), "1,2");
        query.put(ANNOT_CONSEQUENCE_TYPE.key(), "missense_variant,coding_sequence_variant");

        SolrQuery solrQuery = solrQueryParser.parse(query, queryOptions);
        display(query, queryOptions, solrQuery);
        assert((flDefault1 + "&q=*:*&fq=((chromosome:\"1\")+OR+(chromosome:\"2\"))+AND+(soAcc:\"1583\"+OR+soAcc:\"1580\")").equals(solrQuery.toString()));
    }

    @Test
    public void parseAnnotCT4() {
        QueryOptions queryOptions = new QueryOptions();

        Query query = new Query();

        // no consequence types: (xrefs OR regions) but we must add "OR genes", i.e.: xrefs OR regions OR genes
        // we must make an OR with xrefs, genes and regions and add it to the "AND" filter list
        query.put(REGION.key(), "1,2");
        query.put(ANNOT_XREF.key(), "RIPK2,NCF4");

        SolrQuery solrQuery = solrQueryParser.parse(query, queryOptions);
        display(query, queryOptions, solrQuery);
        assert((flDefault1 + "&q=*:*&fq=xrefs:\"RIPK2\"+OR+xrefs:\"NCF4\"+OR+(chromosome:\"1\")+OR+(chromosome:\"2\")").equals(solrQuery.toString()));
    }

    @Test
    public void parseTraits() {
        QueryOptions queryOptions = new QueryOptions();

        Query query = new Query();

        query.put(ANNOT_TRAIT.key(), "melanoma,recessive");

        SolrQuery solrQuery = solrQueryParser.parse(query, queryOptions);
        display(query, queryOptions, solrQuery);
        assert((flDefault1 + "&q=*:*&fq=(traits:\"melanoma\"+OR+traits:\"recessive\")").equals(solrQuery.toString()));
    }

    @Test
    public void parseHPOs() {
        QueryOptions queryOptions = new QueryOptions();

        Query query = new Query();

        query.put(ANNOT_HPO.key(), "HP%3A000365,HP%3A0000007");

        SolrQuery solrQuery = solrQueryParser.parse(query, queryOptions);
        display(query, queryOptions, solrQuery);
        assert((flDefault1 + "&q=*:*&fq=(traits:\"HP%253A000365\"+OR+traits:\"HP%253A0000007\")").equals(solrQuery.toString()));
    }

    @Test
    public void parseClinVars() {
        QueryOptions queryOptions = new QueryOptions();

        Query query = new Query();

        query.put(ANNOT_CLINVAR.key(), "RCV000010071");

        SolrQuery solrQuery = solrQueryParser.parse(query, queryOptions);
        display(query, queryOptions, solrQuery);
        assert((flDefault1 + "&q=*:*&fq=xrefs:\"RCV000010071\"").equals(solrQuery.toString()));
    }

    @Test
    public void parseFormat() {
        QueryOptions queryOptions = new QueryOptions();
        queryOptions.put(QueryOptions.EXCLUDE, VariantField.STUDIES_FILES + "," + VariantField.STUDIES_SAMPLES_DATA);

        Query query = new Query();
        query.put(STUDY.key(), studyName);

        query.put(FORMAT.key(), "NA12877:DP>300;NA12878:DP>500");

        SolrQuery solrQuery = solrQueryParser.parse(query, queryOptions);
        display(query, queryOptions, solrQuery);
        assert((flBase + "&q=*:*&fq=(dp__" + studyName + "__NA12877:{300+TO+*]+AND+dp__" + studyName + "__NA12878:{500+TO+*])").equals(solrQuery.toString()));
    }

    @Test
    public void parseWrongFormat() {
        QueryOptions queryOptions = new QueryOptions();

        Query query = new Query();
        query.put(STUDY.key(), studyName);

        query.put(FORMAT.key(), "NA12877:AC>200");

        try {
            solrQueryParser.parse(query, queryOptions);
        } catch (VariantQueryException e) {
            System.out.println("Success, exception caught!! " + e.getMessage());
            return;
        }
        fail();
    }

    @Test
    public void parseSample() {
        QueryOptions queryOptions = new QueryOptions();
        queryOptions.put(QueryOptions.EXCLUDE, VariantField.STUDIES_FILES);

        Query query = new Query();
        query.put(STUDY.key(), studyName);

        query.put(GENOTYPE.key(), "NA12877:1/0,NA12878:1/1");

        SolrQuery solrQuery = solrQueryParser.parse(query, queryOptions);
        display(query, queryOptions, solrQuery);

        String fl = ",sampleFormat__platinum__NA12877,sampleFormat__platinum__NA12878";
        assert((flBase + fl + "&q=*:*&fq=((gt__" + studyName + "__NA12878:\"1/1\")+OR+(gt__" + studyName + "__NA12877:\"1/0\"))").equals(solrQuery.toString()));
    }

    private void display(Query query, QueryOptions queryOptions, SolrQuery solrQuery) {
        System.out.println("Query        : " + query.toJson());
        System.out.println("Query options: " + queryOptions.toJson());
        System.out.println("Solr query   : " + solrQuery.toString());
        System.out.println();
    }
}
