package com.dharven.fts.repository;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public class User implements Parcelable {
    private String name;
    private double height;
    private double weight;
    private double goalWeight;
    private double age;

    public User(String name, double height, double weight, double goalWeight, double age) {
        this.name = name;
        this.height = height;
        this.weight = weight;
        this.goalWeight = goalWeight;
        this.age = age;
    }

    public User(){}

    protected User(Parcel in) {
        name = in.readString();
        height = in.readDouble();
        weight = in.readDouble();
        goalWeight = in.readDouble();
        age = in.readDouble();
    }

    public static final Creator<User> CREATOR = new Creator<User>() {
        @Override
        public User createFromParcel(Parcel in) {
            return new User(in);
        }

        @Override
        public User[] newArray(int size) {
            return new User[size];
        }
    };

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getHeight() {
        return height;
    }

    public void setHeight(double height) {
        this.height = height;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public double getGoalWeight() {
        return goalWeight;
    }

    public void setGoalWeight(double goalWeight) {
        this.goalWeight = goalWeight;
    }

    public double getAge() {
        return age;
    }

    public void setAge(double age) {
        this.age = age;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeDouble(height);
        dest.writeDouble(weight);
        dest.writeDouble(goalWeight);
        dest.writeDouble(age);
    }
}
