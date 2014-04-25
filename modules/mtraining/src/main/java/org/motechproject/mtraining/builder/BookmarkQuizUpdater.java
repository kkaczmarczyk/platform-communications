package org.motechproject.mtraining.builder;

import org.motechproject.mtraining.domain.Bookmark;
import org.motechproject.mtraining.dto.BookmarkDto;
import org.motechproject.mtraining.dto.ChapterDto;
import org.motechproject.mtraining.dto.CourseDto;
import org.motechproject.mtraining.dto.ModuleDto;
import org.motechproject.mtraining.dto.QuizDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Updater to re-validate and set the quiz in a Bookmark against the provided Course Structure for a given enrollee.
 * Switches Bookmark to next chapter if unable to set a quiz in the Bookmark for the given chapter.
 * @see org.motechproject.mtraining.builder.BookmarkBuilder
 */

@Component
public class BookmarkQuizUpdater {

    private BookmarkBuilder courseBookmarkBuilder;

    @Autowired
    public BookmarkQuizUpdater(BookmarkBuilder bookmarkBuilder) {
        this.courseBookmarkBuilder = bookmarkBuilder;
    }

    /**
     * Given bookmark the API ensures that the current bookmark is valid and if not then updates the bookmark to a valid point
     * 1) If bookmark quiz does not exist, then build bookmark from the chapter
     * 2) If quiz is not active then set bookmark to next active chapter
     * 5) If no next active chapter is left in the module then build bookmark from next active module
     * 5) If no next active module is left in the course then build course completion bookmark
     * @param bookmark
     * @param courseDto
     * @param moduleDto
     * @param chapterDto
     * @return
     */
    public BookmarkDto update(Bookmark bookmark, CourseDto courseDto, ModuleDto moduleDto, ChapterDto chapterDto) {
        QuizDto quiz = chapterDto.getQuiz();
        String externalId = bookmark.getExternalId();

        if (quiz == null) {
            return courseBookmarkBuilder.buildBookmarkFromFirstActiveContent(externalId, courseDto, moduleDto, chapterDto);
        }

        if (!quiz.isActive()) {
            ChapterDto nextActiveChapterDto = moduleDto.getNextActiveChapterAfter(chapterDto.getContentId());
            if (nextActiveChapterDto != null) {
                return courseBookmarkBuilder.buildBookmarkFromFirstActiveContent(externalId, courseDto, moduleDto, nextActiveChapterDto);
            }

            ModuleDto nextActiveModuleDto = courseDto.getNextActiveModuleAfter(moduleDto.getContentId());
            if (nextActiveModuleDto != null) {
                return courseBookmarkBuilder.buildBookmarkFromFirstActiveContent(externalId, courseDto, nextActiveModuleDto);
            }

            return courseBookmarkBuilder.buildCourseCompletionBookmark(externalId, courseDto);
        }

        return courseBookmarkBuilder.buildBookmarkFrom(externalId, courseDto, moduleDto, chapterDto, quiz, bookmark.getDateModified());
    }

}
