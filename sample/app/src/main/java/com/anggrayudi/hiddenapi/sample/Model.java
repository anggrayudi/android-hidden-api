package com.anggrayudi.hiddenapi.sample;

/**
 * Created by Anggrayudi on 11/03/2016.
 */
class Model {

    String source, result, description;

    Model(String source, String result, String description){
        this.source = source;
        this.result = "Result = "+result;
        this.description = description;
    }
}
