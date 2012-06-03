package eu.wimmerinformatik.sks.data;

public class TopicStats {
	private int questionCount;
	private int levels;
	private int[] questionsAtLevel;
	private int currentProgress;
	private int maxProgress;
	
	public int getQuestionCount() {
		return questionCount;
	}
	public void setQuestionCount(int questionCount) {
		this.questionCount = questionCount;
	}
	public int getLevels() {
		return levels;
	}
	public void setLevels(int levels) {
		this.levels = levels;
	}
	public int[] getQuestionsAtLevel() {
		return questionsAtLevel;
	}
	public void setQuestionsAtLevel(int[] questionsAtLevel) {
		this.questionsAtLevel = questionsAtLevel;
	}
	public int getCurrentProgress() {
		return currentProgress;
	}
	public void setCurrentProgress(int currentProgress) {
		this.currentProgress = currentProgress;
	}
	public int getMaxProgress() {
		return maxProgress;
	}
	public void setMaxProgress(int maxProgress) {
		this.maxProgress = maxProgress;
	}

}
