package org.opencb.opencga.catalog.stats.solr;

import org.apache.solr.client.solrj.beans.Field;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by wasim on 27/06/18.
 */
public class SampleSolrModel {

    @Field
    private long uid;

    @Field
    private String studyId;

    @Field
    private String source;

    @Field
    private int release;

    @Field
    private int version;

    @Field
    private int creationYear;

    @Field
    private String creationMonth;

    @Field
    private int creationDay;

    @Field
    private String creationDayOfWeek;

    @Field
    private String status;

    @Field
    private String type;

    @Field
    private boolean somatic;

    @Field
    private List<String> phenotypes;

    @Field
    private List<String> acl;

    @Field
    private List<String> annotationSets;

    @Field("annotations__*")
    private Map<String, Object> annotations;

    public SampleSolrModel() {
        this.annotationSets = new ArrayList<>();
        this.phenotypes = new ArrayList<>();
        this.annotations = new HashMap<>();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("SampleSolrModel{");
        sb.append("uid=").append(uid);
        sb.append(", studyId='").append(studyId).append('\'');
        sb.append(", source='").append(source).append('\'');
        sb.append(", release=").append(release);
        sb.append(", version=").append(version);
        sb.append(", creationYear=").append(creationYear);
        sb.append(", creationMonth='").append(creationMonth).append('\'');
        sb.append(", creationDay=").append(creationDay);
        sb.append(", creationDayOfWeek='").append(creationDayOfWeek).append('\'');
        sb.append(", status='").append(status).append('\'');
        sb.append(", type='").append(type).append('\'');
        sb.append(", somatic=").append(somatic);
        sb.append(", phenotypes=").append(phenotypes);
        sb.append(", acl=").append(acl);
        sb.append(", annotationSets=").append(annotationSets);
        sb.append(", annotations=").append(annotations);
        sb.append('}');
        return sb.toString();
    }

    public long getUid() {
        return uid;
    }

    public SampleSolrModel setUid(long uid) {
        this.uid = uid;
        return this;
    }

    public String getStudyId() {
        return studyId;
    }

    public SampleSolrModel setStudyId(String studyId) {
        this.studyId = studyId;
        return this;
    }

    public String getSource() {
        return source;
    }

    public SampleSolrModel setSource(String source) {
        this.source = source;
        return this;
    }

    public int getRelease() {
        return release;
    }

    public SampleSolrModel setRelease(int release) {
        this.release = release;
        return this;
    }

    public int getVersion() {
        return version;
    }

    public SampleSolrModel setVersion(int version) {
        this.version = version;
        return this;
    }

    public int getCreationYear() {
        return creationYear;
    }

    public SampleSolrModel setCreationYear(int creationYear) {
        this.creationYear = creationYear;
        return this;
    }

    public String getCreationMonth() {
        return creationMonth;
    }

    public SampleSolrModel setCreationMonth(String creationMonth) {
        this.creationMonth = creationMonth;
        return this;
    }

    public int getCreationDay() {
        return creationDay;
    }

    public SampleSolrModel setCreationDay(int creationDay) {
        this.creationDay = creationDay;
        return this;
    }

    public String getCreationDayOfWeek() {
        return creationDayOfWeek;
    }

    public SampleSolrModel setCreationDayOfWeek(String creationDayOfWeek) {
        this.creationDayOfWeek = creationDayOfWeek;
        return this;
    }

    public String getStatus() {
        return status;
    }

    public SampleSolrModel setStatus(String status) {
        this.status = status;
        return this;
    }

    public String getType() {
        return type;
    }

    public SampleSolrModel setType(String type) {
        this.type = type;
        return this;
    }

    public boolean isSomatic() {
        return somatic;
    }

    public SampleSolrModel setSomatic(boolean somatic) {
        this.somatic = somatic;
        return this;
    }

    public List<String> getPhenotypes() {
        return phenotypes;
    }

    public SampleSolrModel setPhenotypes(List<String> phenotypes) {
        this.phenotypes = phenotypes;
        return this;
    }

    public List<String> getAcl() {
        return acl;
    }

    public SampleSolrModel setAcl(List<String> acl) {
        this.acl = acl;
        return this;
    }

    public List<String> getAnnotationSets() {
        return annotationSets;
    }

    public SampleSolrModel setAnnotationSets(List<String> annotationSets) {
        this.annotationSets = annotationSets;
        return this;
    }

    public Map<String, Object> getAnnotations() {
        return annotations;
    }

    public SampleSolrModel setAnnotations(Map<String, Object> annotations) {
        this.annotations = annotations;
        return this;
    }
}

