package io.precognito.services.fixture;


import com.oracle.svm.core.annotate.AutomaticFeature;
import com.oracle.svm.core.jni.JNIRuntimeAccess;
import org.graalvm.nativeimage.hosted.Feature;
import org.rocksdb.RocksDBException;
import org.rocksdb.Status;

/**
 * From: https://github.com/quarkusio/quarkus/issues/7066
 */
@AutomaticFeature
class JNIRegistrationFeature implements Feature {

    @Override
    public void beforeAnalysis(BeforeAnalysisAccess access) {
        JNIRuntimeAccess.register(RocksDBException.class);
        JNIRuntimeAccess.register(RocksDBException.class.getConstructors());
        JNIRuntimeAccess.register(Status.class);
        JNIRuntimeAccess.register(Status.class.getDeclaredConstructors());
    }
}
