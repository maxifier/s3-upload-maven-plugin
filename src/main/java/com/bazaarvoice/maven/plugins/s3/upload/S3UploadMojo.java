package com.bazaarvoice.maven.plugins.s3.upload;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.internal.StaticCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.Upload;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Mojo(name = "s3-upload")
public class S3UploadMojo extends AbstractMojo
{
  /** Access key for S3. */
  @Parameter(property = "s3-upload.accessKey")
  private String accessKey;

  /** Secret key for S3. */
  @Parameter(property = "s3-upload.secretKey")
  private String secretKey;

  /** Execute all steps up except the upload to the S3.  This can be set to true to perform a "dryRun" execution. */
  @Parameter(property = "s3repo.doNotUpload", defaultValue = "false")
  private boolean doNotUpload;

  /** The file to upload. */
  @Parameter(property = "s3-upload.sourceFile")
  private String sourceFile;

  /** The bucket to upload into. */
  @Parameter(property = "s3-upload.bucketName", required = true)
  private String bucketName;

  /** The file (in the bucket) to create. */
  @Parameter(property = "s3-upload.destinationFile")
  private String destinationFile;

  /** Force override of endpoint for S3 regions such as EU. */
  @Parameter(property = "s3-upload.endpoint")
  private String endpoint;

  @Parameter
  private List<FileUploadDescriptor> files;

  @Override
  public void execute() throws MojoExecutionException
  {
    if (files == null) {
        files = new ArrayList<FileUploadDescriptor>();
    }
    if (sourceFile != null) {
      FileUploadDescriptor d = new FileUploadDescriptor();
      d.setSource(sourceFile);
      d.setDestination(destinationFile);
      files.add(d);
    }

    if (files.isEmpty()) {
      throw new MojoExecutionException("No files specified for upload");
    }

    for (FileUploadDescriptor descriptor : files) {
      if (!new File(descriptor.getSource()).exists()) {
        throw new MojoExecutionException("File doesn't exist: " + descriptor.getSource());
      }
      if (descriptor.getDestination() == null) {
        throw new MojoExecutionException("Destination file is not set for " + descriptor.getSource());
      }
    }

    AmazonS3 s3 = getS3Client(accessKey, secretKey);
    if (endpoint != null) {
      s3.setEndpoint(endpoint);
    }

    if (!s3.doesBucketExist(bucketName)) {
      throw new MojoExecutionException("Bucket doesn't exist: " + bucketName);
    }

    if (!doNotUpload) {
      for (FileUploadDescriptor descriptor : files) {
        upload(s3, bucketName, descriptor.getDestination(), descriptor.getSource());
      }
    }
  }

  private static AmazonS3 getS3Client(String accessKey, String secretKey)
  {
    AWSCredentialsProvider provider;
    if (accessKey != null && secretKey != null) {
      AWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
      provider = new StaticCredentialsProvider(credentials);
    } else {
      provider = new DefaultAWSCredentialsProviderChain();
    }

    return new AmazonS3Client(provider);
  }

  private void upload(AmazonS3 s3, String bucketName, String destinationFile, String sourceFile) throws MojoExecutionException {
    File source = new File(sourceFile);
    TransferManager mgr = new TransferManager(s3);
    Upload upload = mgr.upload(bucketName, destinationFile, source);

    try {
      upload.waitForUploadResult();
    } catch (InterruptedException e) {
      throw new MojoExecutionException("Unable to upload file " + source + " to s3://" + bucketName + "/" + destinationFile);
    }

    getLog().info("File " + source + " uploaded to s3://" + bucketName + "/" + destinationFile);
  }
}
