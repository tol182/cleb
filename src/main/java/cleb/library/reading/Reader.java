package cleb.library.reading;

import static cleb.library.reading.util.ReaderUtil.getReaderReference;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * This servlet class transfers request read book to appropriate reader.
 */
public class Reader extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private String folderPath;

    @Override
    public void init() throws ServletException {
        // Load properties
        Properties properties = new Properties();

        try (InputStream propIn = getServletContext()
            .getResourceAsStream("/WEB-INF/classes/props.properties")) {
            properties.load(propIn);
        } catch (IOException e) {
            // Silently ignore
        }

        // Directory with books
        folderPath = properties.getProperty("book-store");
    }

    @Override
    protected void doGet(HttpServletRequest request,
        HttpServletResponse response) throws ServletException, IOException {

        // Show library
        RequestDispatcher dispatcher = request.getRequestDispatcher("/library");
        dispatcher.forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request,
        HttpServletResponse response) throws ServletException, IOException {

        String fileName = request.getParameter("filename");
        String fileType = FilenameUtils.getExtension(fileName);

        File bookFile = new File(folderPath + fileName);

        String coverName = request.getParameter("covername");

        request.setAttribute("bookfile", bookFile);
        request.setAttribute("covername", coverName);

        // Get the string reference for appropriate reader
        String reader = getReaderReference(fileType);

        // Forward request with file reference to corresponding reader
        RequestDispatcher dispatcher = request.getRequestDispatcher(reader);
        dispatcher.forward(request, response);
    }

}
