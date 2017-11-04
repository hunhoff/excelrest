
package com.hp.excelrest.src.rallydev;

public class Defect {
	
	private boolean run;
	private boolean postMail;
	private String output;
	private String stateFilter[];
	private String objectID;
	private String formattedID;
	private String name;
	private String State;
	private String Priority;
	private String Severity;
	private String Owner;
	private String Blocked;
	private String submitter;
	private String release;
	private String iteration;
	private String fixedInBuild;
	private String verifiedInBuild;
	private String foundInBuild;
	private String creationDate;
	private String environment;
	private String resolution;

	public Defect(){}

	public boolean isRun() {
		return run;
	}

	public void setRun(boolean run) {
		this.run = run;
	}

	public boolean isPostMail() {
		return postMail;
	}

	public void setPostMail(boolean postMail) {
		this.postMail = postMail;
	}

	public String getOutput() {
		return output;
	}

	public void setOutput(String output) {
		this.output = output;
	}

	public String[] getStateFilter() {
		return stateFilter;
	}

	public void setStateFilter(String[] stateFilter) {
		this.stateFilter = stateFilter;
	}

	public String getObjectID() {
		return objectID;
	}

	public void setObjectID(String objectID) {
		this.objectID = objectID;
	}

	public String getFormattedID() {
		return formattedID;
	}

	public void setFormattedID(String formattedID) {
		this.formattedID = formattedID;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getState() {
		return State;
	}

	public void setState(String state) {
		State = state;
	}

	public String getPriority() {
		return Priority;
	}

	public void setPriority(String priority) {
		Priority = priority;
	}

	public String getSeverity() {
		return Severity;
	}

	public void setSeverity(String severity) {
		Severity = severity;
	}

	public String getOwner() {
		return Owner;
	}

	public void setOwner(String owner) {
		Owner = owner;
	}

	public String getBlocked() {
		return Blocked;
	}

	public void setBlocked(String blocked) {
		Blocked = blocked;
	}

	public String getSubmitter() {
		return submitter;
	}

	public void setSubmitter(String submitter) {
		this.submitter = submitter;
	}

	public String getRelease() {
		return release;
	}

	public void setRelease(String release) {
		this.release = release;
	}

	public String getIteration() {
		return iteration;
	}

	public void setIteration(String iteration) {
		this.iteration = iteration;
	}

	public String getFixedInBuild() {
		return fixedInBuild;
	}

	public void setFixedInBuild(String fixedInBuild) {
		this.fixedInBuild = fixedInBuild;
	}

	public String getVerifiedInBuild() {
		return verifiedInBuild;
	}

	public void setVerifiedInBuild(String verifiedInBuild) {
		this.verifiedInBuild = verifiedInBuild;
	}

	public String getFoundInBuild() {
		return foundInBuild;
	}

	public void setFoundInBuild(String foundInBuild) {
		this.foundInBuild = foundInBuild;
	}

	public String getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(String creationDate) {
		this.creationDate = creationDate;
	}

	public String getEnvironment() {
		return environment;
	}

	public void setEnvironment(String environment) {
		this.environment = environment;
	}

	public String getResolution() {
		return resolution;
	}

	public void setResolution(String resolution) {
		this.resolution = resolution;
	}
}