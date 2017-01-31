package com.anggrayudi.hiddenapi;

import android.content.res.Resources;

/**
 * Created by Anggrayudi on 11/03/2016.<p>
 * Exception class to handle resource that is not found.
 */
class ResourceNotFoundException extends Resources.NotFoundException {

    ResourceNotFoundException(String message){
        super(message);
    }
}
