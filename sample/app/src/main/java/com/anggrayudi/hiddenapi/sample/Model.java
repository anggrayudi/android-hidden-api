package com.anggrayudi.hiddenapi.sample;

/**
 * Created by Anggrayudi on 11/03/2016.
 */
public class Model {

    public String source, result, description;

    public Model(String source, String result, String description){
        this.source = source;
        this.result = "Result = "+result;
        this.description = description;
    }
}
