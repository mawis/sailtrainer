/*  vim: set sw=4 tabstop=4 fileencoding=UTF-8:
 *
 *  Copyright 2014 Matthias Wimmer
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package eu.wimmerinformatik.sbfb.data;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

/**
 * POJO that holds a question and the answers and the current practice state for
 * it.
 *
 * TODO: Separate the question/answer from the practice state. We wouldn't need
 * the question and answer in the database, but could keep them in resources.
 * Also instead of keeping a level, I have to think if I store the history of
 * good and bad answers. This would be useful to build the planned
 * syncronisation with the web trainer.
 *
 * @author Matthias Wimmer
 */
public class Question {
	/**
	 * ID of the question.
	 *
	 * This is the same ID as on the web trainer.
	 */
	private int id;

	/**
	 * This is the grouping of questions to a handful of topics, that we
	 * present for selection on the start screen.
	 */
	private int topicId;

	/**
	 * Textual reference in the questionaire.
	 */
	private String reference;

	/**
	 * Text of the question.
	 */
	private String questionText;

	/**
	 * Possible answers.
	 *
	 * The first one is the correct answer.
	 */
	private List<String> answers = new LinkedList<String>();

	/**
	 * How often has the question been answered correctly.
	 *
	 * = (correct answers) - (incorrect answers)
	 */
	private int level;

	/**
	 * When will we ask the question the next time.
	 */
	private Date nextTime;

	/**
	 * Getter for the ID of the question.
	 *
	 * This is the same ID as on the web trainer.
	 *
	 * @return the ID of the question
	 */
	public int getId() {
		return id;
	}

	/**
	 * Setter for the ID of the question.
	 *
	 * This is the same ID as on the web trainer.
	 *
	 * @param id the ID of the question
	 */
	public void setId(final int id) {
		this.id = id;
	}

	/**
	 * Getter for the topicId.
	 *
	 * This is the grouping of questions to a handful of topics, that we
	 * present for selection on the start screen.
	 *
	 * @return the ID of the topic this question belongs to
	 */
	public int getTopicId() {
		return topicId;
	}

	/**
	 * Setter for the topicId.
	 *
	 * This is the grouping of questions to a handful of topics, that we
	 * present for selection on the start screen.
	 *
	 * @param topicId the ID of the topic this question belongs to
	 */
	public void setTopicId(final int topicId) {
		this.topicId = topicId;
	}

	/**
	 * Getter for the textual reference in the questionaire.
	 *
	 * @return textual reference in the questionaire
	 */
	public String getReference() {
		return reference;
	}

	/**
	 * Setter for the textual reference in the questionaire.
	 *
	 * @param reference textual reference in the questionaire
	 */
	public void setReference(final String reference) {
		this.reference = reference;
	}

	/**
	 * Getter for the text of the question.
	 *
	 * @return question text
	 */
	public String getQuestionText() {
		return questionText;
	}

	/**
	 * Setter for the text of the question.
	 *
	 * @param questionText the question text to set
	 */
	public void setQuestionText(final String questionText) {
		this.questionText = questionText;
	}

	/**
	 * Getter for the possible answers.
	 *
	 * The first one is the correct answer.
	 *
	 * @return list of answers (the first is the correct one)
	 */
	public List<String> getAnswers() {
		return answers;
	}

	/**
	 * Setter for the possible answers.
	 *
	 * The first one is the correct answer.
	 *
	 * @param answers list of answers (the first has to be the correct one)
	 */
	public void setAnswers(final List<String> answers) {
		this.answers = answers;
	}

	/**
	 * Getter for how often has the question been answered correctly.
	 *
	 * @return (correct answers) - (incorrect answers)
	 */
	public int getLevel() {
		return level;
	}

	/**
	 * Setter for how often has the question been answered correctly.
	 *
	 * @param level (correct answers) - (incorrect answers)
	 */
	public void setLevel(final int level) {
		this.level = level;
	}

	/**
	 * Getter for when will we ask the question the next time.
	 *
	 * @return when the question may be asked again
	 */
	public Date getNextTime() {
		return nextTime;
	}

	/**
	 * Setter for when will we ask the question the next time.
	 *
	 * @param nextTime when the question may be asked again
	 */
	public void setNextTime(final Date nextTime) {
		this.nextTime = nextTime;
	}
}
