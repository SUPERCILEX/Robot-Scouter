//import kotlin.js.Promise.Companion.resolve
//
//function deleteCollection(db, collectionPath, batchSize) {
//    var collectionRef = db.collection(collectionPath);
//    var query = collectionRef.orderBy('__name__').limit(batchSize);
//
//    return new Promise ((resolve, reject) => {
//        deleteQueryBatch(db, query, batchSize, resolve, reject);
//    });
//}
//
//function deleteQueryBatch(db, query, batchSize, resolve, reject) {
//    query.get()
//            .then((snapshot) => {
//                // When there are no documents left, we are done
//                if (snapshot.size == 0) {
//                    return 0;
//                }
//
//                // Delete documents in a batch
//                var batch = db.batch();
//                snapshot.docs.forEach((doc) => {
//                    batch.delete(doc.ref);
//                });
//
//                return batch.commit().then(() => {
//                    return snapshot.size;
//                });
//            }).then((numDeleted) => {
//        if (numDeleted === 0) {
//            resolve();
//            return;
//        }
//
//        // Recurse on the next process tick, to avoid
//        // exploding the stack.
//        process.nextTick(() => {
//            deleteQueryBatch(db, query, batchSize, resolve, reject);
//        });
//    })
//    .catch(reject);
//}
