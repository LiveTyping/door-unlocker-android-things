package com.iot.doorunlocker.data;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class RecognizeResponse {

	@SerializedName("added_at")
	@Expose
	private String addedAt;

	@SerializedName("distance")
	@Expose
	private double distance;

	@SerializedName("group_id")
	@Expose
	private String groupId;

	@SerializedName("image_id")
	@Expose
	private String imageId;

	@SerializedName("person_id")
	@Expose
	private String personId;

	public void setAddedAt(String addedAt){
		this.addedAt = addedAt;
	}

	public String getAddedAt(){
		return addedAt;
	}

	public void setDistance(double distance){
		this.distance = distance;
	}

	public double getDistance(){
		return distance;
	}

	public void setGroupId(String groupId){
		this.groupId = groupId;
	}

	public String getGroupId(){
		return groupId;
	}

	public void setImageId(String imageId){
		this.imageId = imageId;
	}

	public String getImageId(){
		return imageId;
	}

	public void setPersonId(String personId){
		this.personId = personId;
	}

	public String getPersonId(){
		return personId;
	}

	@Override
 	public String toString(){
		return 
			"Response{" + 
			"added_at = '" + addedAt + '\'' + 
			",distance = '" + distance + '\'' + 
			",group_id = '" + groupId + '\'' + 
			",image_id = '" + imageId + '\'' + 
			",person_id = '" + personId + '\'' + 
			"}";
		}
}