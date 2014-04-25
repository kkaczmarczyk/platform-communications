package org.motechproject.mtraining.repository;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.ektorp.CouchDbConnector;
import org.ektorp.ViewQuery;
import org.ektorp.support.View;
import org.motechproject.commons.couchdb.dao.MotechBaseRepository;
import org.motechproject.mtraining.domain.EnrolleeCourseProgress;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for {@link org.motechproject.mtraining.domain.EnrolleeCourseProgress} couch document
 */

@Repository
public class AllEnrolleeCourseProgress extends MotechBaseRepository<EnrolleeCourseProgress> {

    @Autowired
    public AllEnrolleeCourseProgress(@Qualifier("mtrainingDbConnector") CouchDbConnector db) {
        super(EnrolleeCourseProgress.class, db);
        initStandardDesignDocument();
    }

    @View(name = "by_externalId", map = "function(doc) { if (doc.type ==='EnrolleeCourseProgress') { emit(doc.externalId, doc._id); }}")
    public List<EnrolleeCourseProgress> findBy(String externalId) {
        if (externalId == null) {
            return null;
        }
        ViewQuery viewQuery = createQuery("by_externalId").key(externalId).includeDocs(true);
        return db.queryView(viewQuery, EnrolleeCourseProgress.class);
    }

    /**
     * Returns the course progress for which course status is not CLOSED.
     * Assuming here that at a given point of time enrollee will have only one course in which he or she is enrolled
     * Course is not considered done till status is not Closed
     *
     * @param externalId
     * @return
     */
    public EnrolleeCourseProgress findCourseProgressForOngoingCourse(String externalId) {
        List<EnrolleeCourseProgress> enrolleeCourseProgressList = findBy(externalId);
        return (EnrolleeCourseProgress) CollectionUtils.find(enrolleeCourseProgressList, new Predicate() {
            @Override
            public boolean evaluate(Object object) {
                EnrolleeCourseProgress enrolleeCourseProgress = (EnrolleeCourseProgress) object;
                return enrolleeCourseProgress.notClosed();
            }
        });
    }

    public EnrolleeCourseProgress findBy(String externalId, final UUID courseIdentifier) {
        if (externalId == null || courseIdentifier == null) {
            return null;
        }

        List<EnrolleeCourseProgress> enrolleeCourseProgressList = findBy(externalId);
        if (CollectionUtils.isEmpty(enrolleeCourseProgressList)) {
            return null;
        }

        return (EnrolleeCourseProgress) CollectionUtils.find(enrolleeCourseProgressList, new Predicate() {
            @Override
            public boolean evaluate(Object object) {
                EnrolleeCourseProgress enrolleeCourseProgress = (EnrolleeCourseProgress) object;
                return enrolleeCourseProgress.isFor(courseIdentifier);
            }
        });

    }
}
