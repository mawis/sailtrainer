package eu.wimmerinformatik.sks.data;

import java.util.Date;

public class QuestionSelection {
	private int selectedQuestion;
	private boolean finished;
	private Date nextQuestion;
	private int openQuestions;
	private int totalQuestions;
	private int maxProgress;
	private int currentProgress;
	
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
	public int getMaxProgress() {
		return maxProgress;
	}
	public void setMaxProgress(int maxProgress) {
		this.maxProgress = maxProgress;
	}
	public int getCurrentProgress() {
		return currentProgress;
	}
	public void setCurrentProgress(int currentProgress) {
		this.currentProgress = currentProgress;
	}

}
