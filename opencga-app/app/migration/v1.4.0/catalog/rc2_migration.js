// #906
migrateCollection("file", {"_reverse": { $exists: false } }, {name: 1}, function(bulk, doc) {
        bulk.find({"_id": doc._id}).updateOne({"$set": {"_reverse": doc.name.split("").reverse().join("")}});
    }
);

db.file.createIndex({"_reverse": 1, "studyUid": 1, "status.name": 1}, {"background": true});


// #912
db.job.createIndex({"_creationDate": 1, "studyUid": 1, "status.name": 1}, {"background": true});
db.job.createIndex({"_modificationDate": 1, "studyUid": 1, "status.name": 1}, {"background": true});
db.file.createIndex({"_creationDate": 1, "studyUid": 1, "status.name": 1}, {"background": true});
db.file.createIndex({"_modificationDate": 1, "studyUid": 1, "status.name": 1}, {"background": true});
db.sample.createIndex({"_creationDate": 1, "studyUid": 1, "status.name": 1}, {"background": true});
db.sample.createIndex({"_modificationDate": 1, "studyUid": 1, "status.name": 1}, {"background": true});
db.individual.createIndex({"_creationDate": 1, "studyUid": 1, "status.name": 1}, {"background": true});
db.individual.createIndex({"_modificationDate": 1, "studyUid": 1, "status.name": 1}, {"background": true});
db.cohort.createIndex({"_creationDate": 1, "studyUid": 1, "status.name": 1}, {"background": true});
db.cohort.createIndex({"_modificationDate": 1, "studyUid": 1, "status.name": 1}, {"background": true});
db.family.createIndex({"_creationDate": 1, "studyUid": 1, "status.name": 1}, {"background": true});
db.family.createIndex({"_modificationDate": 1, "studyUid": 1, "status.name": 1}, {"background": true});
db.diseasePanel.createIndex({"_creationDate": 1, "studyUid": 1, "status.name": 1}, {"background": true});
db.diseasePanel.createIndex({"_modificationDate": 1, "studyUid": 1, "status.name": 1}, {"background": true});