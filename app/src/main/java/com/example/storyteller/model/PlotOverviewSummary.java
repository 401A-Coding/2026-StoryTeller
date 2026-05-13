package com.example.storyteller.model;

import java.util.ArrayList;
import java.util.List;

public class PlotOverviewSummary {
	private String overallSummary;
	private List<String> mainLine;
	private List<String> turningPoints;
	private List<String> characterThreads;
	private String rhythm;

	public PlotOverviewSummary() {
		this.mainLine = new ArrayList<>();
		this.turningPoints = new ArrayList<>();
		this.characterThreads = new ArrayList<>();
	}

	public String getOverallSummary() {
		return overallSummary;
	}

	public void setOverallSummary(String overallSummary) {
		this.overallSummary = overallSummary;
	}

	public List<String> getMainLine() {
		return mainLine;
	}

	public void setMainLine(List<String> mainLine) {
		this.mainLine = mainLine == null ? new ArrayList<>() : mainLine;
	}

	public List<String> getTurningPoints() {
		return turningPoints;
	}

	public void setTurningPoints(List<String> turningPoints) {
		this.turningPoints = turningPoints == null ? new ArrayList<>() : turningPoints;
	}

	public List<String> getCharacterThreads() {
		return characterThreads;
	}

	public void setCharacterThreads(List<String> characterThreads) {
		this.characterThreads = characterThreads == null ? new ArrayList<>() : characterThreads;
	}

	public String getRhythm() {
		return rhythm;
	}

	public void setRhythm(String rhythm) {
		this.rhythm = rhythm;
	}
}

