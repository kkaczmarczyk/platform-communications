package org.motechproject.mtraining.domain;

import org.codehaus.jackson.annotate.JsonProperty;
import org.ektorp.support.TypeDiscriminator;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Couch document object representing a Quiz content.
 * + name    : quiz name
 * + contentId   : UUID that for a quiz (different from the _id generated by couch)
 * + version     : quiz version (a quiz can have multiple versions, different versions of the quiz will have same contentId)
 * + externalContentId  : Id that points to an external file or resource that is associated with the quiz.For eg. an audio file that is played to the enrollee
 * + createdBy    : Author of the quiz
 * + createdOn    : Date on which quiz was created
 * + passPercentage : Pass percentage for the quiz
 * + strategy       : strategy for selecting quiz questions.
 * + numberOfQuizQuestionsToBePlayed : Number of quiz questions to be played to the enrollee taking the quiz. Its value will be lesser than or equal to total number of questions available in the quiz.
 */
@TypeDiscriminator("doc.type === 'Quiz'")
public class Quiz extends Content {

    @JsonProperty
    private String name;

    @JsonProperty
    private List<Question> questions;

    @JsonProperty
    private Integer numberOfQuizQuestionsToBePlayed;

    @JsonProperty
    private Double passPercentage;

    @JsonProperty
    private String strategy;

    Quiz() {
    }

    public Quiz(boolean isActive, String name, String externalContentId, List<Question> questions, Integer numberOfQuizQuestionsToBePlayed, Double passPercentage, String createdBy) {
        super(isActive, externalContentId, createdBy);
        this.name = name;
        this.questions = questions;
        this.numberOfQuizQuestionsToBePlayed = numberOfQuizQuestionsToBePlayed;
        this.passPercentage = passPercentage;
        this.strategy = "RANDOM";
    }

    public Quiz(UUID contentId, Integer version, boolean isActive, String name, String externalContentId, List<Question> questions, Integer numberOfQuizQuestionsToBePlayed, Double passPercentage, String createdBy) {
        super(contentId, version, isActive, externalContentId, createdBy);
        this.name = name;
        this.questions = questions;
        this.numberOfQuizQuestionsToBePlayed = numberOfQuizQuestionsToBePlayed;
        this.passPercentage = passPercentage;
        this.strategy = "RANDOM";
    }

    public String getName() {
        return name;
    }

    public Integer getNumberOfQuizQuestionsToBePlayed() {
        return numberOfQuizQuestionsToBePlayed;
    }

    /**
     * Returning a copy of questions so that questions are not changed outside of the quiz
     *
     * @return
     */
    public List<Question> getQuestions() {
        return questions == null ? new ArrayList<Question>() : copyQuestions();
    }

    public Double getPassPercentage() {
        return passPercentage;
    }

    public String getStrategy() {
        return strategy;
    }

    public Boolean isPassed(Double score) {
        return score >= passPercentage;
    }

    public Question findQuestion(UUID questionId) {
        return (Question) Content.findContentByContentId(questions, questionId);
    }

    private List<Question> copyQuestions() {
        return new ArrayList<>(questions);
    }

}
