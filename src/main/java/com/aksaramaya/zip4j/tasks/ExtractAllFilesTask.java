package com.aksaramaya.zip4j.tasks;

import com.aksaramaya.zip4j.io.inputstream.SplitInputStream;
import com.aksaramaya.zip4j.io.inputstream.ZipInputStream;
import com.aksaramaya.zip4j.model.FileHeader;
import com.aksaramaya.zip4j.model.ZipModel;
import com.aksaramaya.zip4j.progress.ProgressMonitor;
import com.aksaramaya.zip4j.tasks.ExtractAllFilesTask.ExtractAllFilesTaskParameters;
import com.aksaramaya.zip4j.util.UnzipUtil;

import java.io.IOException;
import java.nio.charset.Charset;

import static com.aksaramaya.zip4j.headers.HeaderUtil.getTotalUncompressedSizeOfAllFileHeaders;

public class ExtractAllFilesTask extends AbstractExtractFileTask<ExtractAllFilesTaskParameters> {

  private char[] password;
  private SplitInputStream splitInputStream;

  public ExtractAllFilesTask(ZipModel zipModel, char[] password, AsyncTaskParameters asyncTaskParameters) {
    super(zipModel, asyncTaskParameters);
    this.password = password;
  }

  @Override
  protected void executeTask(ExtractAllFilesTaskParameters taskParameters, ProgressMonitor progressMonitor)
      throws IOException {
    try (ZipInputStream zipInputStream = prepareZipInputStream(taskParameters.charset)) {
      for (FileHeader fileHeader : getZipModel().getCentralDirectory().getFileHeaders()) {
        if (fileHeader.getFileName().startsWith("__MACOSX")) {
          progressMonitor.updateWorkCompleted(fileHeader.getUncompressedSize());
          continue;
        }

        splitInputStream.prepareExtractionForFileHeader(fileHeader);

        extractFile(zipInputStream, fileHeader, taskParameters.outputPath, null, progressMonitor);
        verifyIfTaskIsCancelled();
      }
    } finally {
      if (splitInputStream != null) {
        splitInputStream.close();
      }
    }
  }

  @Override
  protected long calculateTotalWork(ExtractAllFilesTaskParameters taskParameters) {
    return getTotalUncompressedSizeOfAllFileHeaders(getZipModel().getCentralDirectory().getFileHeaders());
  }

  private ZipInputStream prepareZipInputStream(Charset charset) throws IOException {
    splitInputStream = UnzipUtil.createSplitInputStream(getZipModel());

    FileHeader fileHeader = getFirstFileHeader(getZipModel());
    if (fileHeader != null) {
      splitInputStream.prepareExtractionForFileHeader(fileHeader);
    }

    return new ZipInputStream(splitInputStream, password, charset);
  }

  private FileHeader getFirstFileHeader(ZipModel zipModel) {
    if (zipModel.getCentralDirectory() == null
        || zipModel.getCentralDirectory().getFileHeaders() == null
        || zipModel.getCentralDirectory().getFileHeaders().size() == 0) {
      return null;
    }

    return zipModel.getCentralDirectory().getFileHeaders().get(0);
  }

  public static class ExtractAllFilesTaskParameters extends AbstractZipTaskParameters {
    private String outputPath;

    public ExtractAllFilesTaskParameters(String outputPath, Charset charset) {
      super(charset);
      this.outputPath = outputPath;
    }
  }

}
