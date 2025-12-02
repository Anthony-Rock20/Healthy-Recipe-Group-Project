package com.example.peakplatesapp;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class AccessDataView{

    private final StringProperty userName = new SimpleStringProperty();
    private final StringProperty userAge = new SimpleStringProperty();
    private final StringProperty userHeight = new SimpleStringProperty();
    private final StringProperty userWeight = new SimpleStringProperty();
    private final StringProperty userGender = new SimpleStringProperty();
    private final ReadOnlyBooleanWrapper writePossible = new ReadOnlyBooleanWrapper();

    public AccessDataView() {
        writePossible.bind(userName.isNotEmpty()
                .and(userAge.isNotEmpty())
                .and(userHeight.isNotEmpty())
                .and(userWeight.isNotEmpty())
                .and(userGender.isNotEmpty()));
    }

    public StringProperty userNameProperty () {
        return userName;
    }

    public StringProperty userAgeProperty() { return userAge;}

    public StringProperty userHeightProperty() {
        return userHeight;
    }

    public StringProperty userWeightProperty() {
        return userWeight;
    }

    public StringProperty userGenderProperty() {
        return userGender;
    }

    public ReadOnlyBooleanProperty isWritePossibleProperty() {
        return writePossible.getReadOnlyProperty();
    }
}
