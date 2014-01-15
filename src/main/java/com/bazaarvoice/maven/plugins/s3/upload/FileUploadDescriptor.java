/*
 * Copyright (c) 2008-2014 Maxifier Ltd. All Rights Reserved.
 */
package com.bazaarvoice.maven.plugins.s3.upload;

/**
 * FileUploadDescriptor
 *
 * @author Alexander Kochurov (alexander.kochurov@maxifier.com) (2014-01-14 12:49)
 */
public class FileUploadDescriptor {
    private String source;
    private String destination;

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }
}
