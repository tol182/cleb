package cleb.saving;

import org.apache.commons.io.FileUtils;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoField;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;

/**
 * This servlet stores basic information about epub book in database and places
 * the book into storing directory.
 */
// TODO remove e.printstacktraces
public class EPUBSaver extends HttpServlet implements ISaver {

    private static final long serialVersionUID = 1L;

    private String tempFolderPath;

    private String folderPath;

    private Object book;

    /**
     * Initializes temporary and storing directories.
     */
    @Override
    public void init() throws ServletException {
        // Directory for temporary storing uploaded books
        tempFolderPath = getServletContext()
                .getInitParameter("file-temp-upload");
        // Directory to store uploaded books
        folderPath = getServletContext().getInitParameter("file-store");
    }

    @Override
    protected void doPost(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {
        String tempBookPath = tempFolderPath
                + (String) request.getAttribute("file");
        String bookPath = folderPath + (String) request.getAttribute("file");

        book = request.getAttribute("book");

        if (getBasicInfo(request, book)) {
            storeInDir(tempBookPath, bookPath);
        } else {
            // TODO show user error page
        }

    }

    @Override
    public boolean getBasicInfo(HttpServletRequest request, Object book) {

        // Information about file, will go into db
        String fileName = (String) request.getAttribute("file");
        String md5 = (String) request.getAttribute("md5");
        Long fileSize = (Long) request.getAttribute("size");
        String fileType = (String) request.getAttribute("type");

        // Information about book, will go into db
        String genre = "";
        String authorFirstName = "";
        String authorLastName = "";
        String title = "";
        String seqName = "";
        String seqNumber = "";
        String published = "";
        // FIXME add logic to identify uploader
        String uploadedBy = "ADMIN";

        // Necessary cast to process with book
        ZipFile zip = (ZipFile) book;

        // Add random suffix to temporary directory based on current time
        String suffix = String
                .valueOf(Instant.now().get(ChronoField.MILLI_OF_SECOND));

        // Extract META-INF/container.xml and get the location of content.opf
        // It's location inside book differs from book to book
        // content.opf has the neccessary information about the book
        String containerxmlPath = tempFolderPath + "/" + suffix + "/";

        try {
            zip.extractFile("META-INF/container.xml", containerxmlPath);

            SAXBuilder builder = new SAXBuilder();

            Document containerxmlDoc = builder.build(
                    new File(containerxmlPath + "/META-INF/container.xml"));

            // container.xml root and namespace
            Element containerxmlRoot = containerxmlDoc.getRootElement();
            Namespace containerxmlNs = containerxmlRoot.getNamespace();

            // content.opf location inside epub book
            String contentopfPath = containerxmlRoot
                    .getChild("rootfiles", containerxmlNs)
                    .getChild("rootfile", containerxmlNs)
                    .getAttributeValue("full-path");

            zip.extractFile(contentopfPath, containerxmlPath);

            Document contentopfDoc = builder
                    .build(new File(containerxmlPath + contentopfPath));

            // content.opf root and namespace
            Element contentopfRoot = contentopfDoc.getRootElement();
            Namespace contentopfNs = contentopfRoot.getNamespace();
            Namespace dc = contentopfRoot.getNamespace("dc");

            // Get required elements to extract necessary information about book
            Element metadataEl = null;
            Element titleEl = null;
            Element creatorEl = null;

            // Not all books have all these items, that's why every one
            // statement below covered in try catch block
            try {
                metadataEl = contentopfRoot.getChild("metadata", contentopfNs);
            } catch (NullPointerException e) {
            }

            try {
                titleEl = metadataEl.getChild("title", dc);
                title = titleEl.getText();
            } catch (NullPointerException e) {
            }

            try {
                creatorEl = metadataEl.getChild("creator", dc);
                authorFirstName = creatorEl.getText();
                authorLastName = creatorEl.getText();
            } catch (NullPointerException e) {
            }

        } catch (ZipException | JDOMException | IOException e) {
            e.printStackTrace();
        } finally {
            cleanTmpDir(containerxmlPath);
        }

        return storeInDB(fileName, md5, fileSize, fileType, genre,
                authorFirstName, authorLastName, title, seqName, seqNumber,
                published, uploadedBy);
    }

    /**
     * This method deletes temporarily extracted files from epub book.
     *
     * @param path
     *        Directory to delete
     */
    private void cleanTmpDir(String path) {
        FileUtils.deleteQuietly(new File(path));
    }
}