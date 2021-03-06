package cleb.uploading.validating;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;

/**
 * This servlet class validates new epub books.
 */
public class EPUBValidator extends HttpServlet implements IValidator {

    private static final long serialVersionUID = 1L;

    // Logger for this class
    private static final Logger logger = LogManager
        .getLogger(EPUBValidator.class.getName());

    private String tempFolderPath;

    private String errorDesc;

    @Override
    public void init() throws ServletException {
        // Load properties
        Properties properties = new Properties();

        try (InputStream propIn = getServletContext()
            .getResourceAsStream("/WEB-INF/classes/props.properties")) {
            properties.load(propIn);
        } catch (IOException e) {
            logger.error("Can not load properties", e);
        }

        // Directory for temporary storing uploaded books - till it's checked by
        // this servlet
        tempFolderPath = properties.getProperty("file-temp-upload");

        // Error description when file is not a valid epub book
        errorDesc = properties.getProperty("epub-validator-error");

        logger.info("EPUBValidator initialized");
    }

    @Override
    protected void doPost(HttpServletRequest request,
        HttpServletResponse response) throws ServletException, IOException {

        String fileName = (String) request.getAttribute("file");

        Object book = validateBook(fileName);

        if (book != null) {
            request.setAttribute("book", book);
            RequestDispatcher dispatcher = request
                .getRequestDispatcher("/EPUBSaver");
            dispatcher.forward(request, response);
        } else {
            // Inform user about error
            request.setAttribute("errordesc", errorDesc);
            request.setAttribute("previouspage", "/upload");

            RequestDispatcher dispatcher = getServletContext()
                .getRequestDispatcher("/error");

            dispatcher.forward(request, response);
        }
    }

    @Override
    public Object validateBook(String fileName) {

        ZipFile book = null;

        File tempBookFile = new File(tempFolderPath + fileName);

        // Valid epub book should be valid zip archive with certain directory
        // structure inside
        try {
            book = new ZipFile(tempBookFile);

            if (book.isValidZipFile()) {
                try {
                    // Any valid epub book should contain these two files
                    book.getFileHeader("META-INF/container.xml").getFileName();
                    book.getFileHeader("mimetype").getFileName();

                    logger.info("Book \"{}\" successfully validated", fileName);
                } catch (NullPointerException e) {
                    logger.error("Book \"{}\" is not a valid epub book",
                        fileName, e);

                    return null;
                }
            } else {
                logger.warn("Book \"{}\" is not a valid epub book", fileName);

                return null;
            }
        } catch (ZipException e) {
            logger.error("Can not validate book \"{}\"", fileName, e);
        }

        return book;
    }

}
