package eu.wimmerinformatik.sbfs.data;

import java.util.Date;

public class QuestionSelection {
	private int selectedQuestion;
	private boolean finished;
	private Date nextQuestion;
	private int openQuestions;
	private int totalQuestions;
	public int getSelectedQuestion() {
		return selectedQuestion;
	}
	public void setSelectedQuestion(int selectedQuestion) {
		this.selectedQuestion = selectedQuestion;
	}
	public boolean isFinished() {
		return finished;
	}
	public void setFinished(boolean finished) {
		this.finished = finished;
	}
	public Date getNextQuestion() {
		return nextQuestion;
	}
	public void setNextQuestion(Date nextQuestion) {
		this.nextQuestion = nextQuestion;
	}
	public int getOpenQuestions() {
		return openQuestions;
	}
	public void setOpenQuestions(int openQuestions) {
		this.openQuestions = openQuestions;
	}
	public int getTotalQuestions() {
		return totalQuestions;
	}
	public void setTotalQuestions(int totalQuestions) {
		this.totalQuestions = totalQuestions;
	}

}
