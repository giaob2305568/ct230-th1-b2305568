package vn.edu.bookstore.legacy.adapter.soap;

import jakarta.xml.ws.WebServiceException;
import jakarta.xml.ws.soap.SOAPFaultException;
import java.util.List;
import org.springframework.stereotype.Component;
import vn.edu.bookstore.legacy.adapter.soap.generated.Book;
import vn.edu.bookstore.legacy.adapter.soap.generated.BookCatalogFault;
import vn.edu.bookstore.legacy.adapter.soap.generated.BookCatalogPortType;
import vn.edu.bookstore.legacy.adapter.soap.generated.ErrorDetail;
import vn.edu.bookstore.legacy.adapter.soap.generated.GetBookRequest;
import vn.edu.bookstore.legacy.adapter.soap.generated.GetBookResponse;
import vn.edu.bookstore.legacy.adapter.soap.generated.SearchBooksRequest;
import vn.edu.bookstore.legacy.adapter.soap.generated.SearchBooksResponse;

/**
 * Tầng chống ăn mòn (anti-corruption layer): gọi BookCatalogService và chuyển
 * kiểu dữ liệu + lỗi SOAP sang mô hình nội bộ {@link LegacyBook} / {@link LegacyCatalogException}.
 */
@Component
public class LegacyCatalogClient {

    private final BookCatalogPortType port;

    public LegacyCatalogClient(BookCatalogPortType port) {
        this.port = port;
    }

    public LegacyBook getBook(String isbn) {
        // TODO(TH1-6): tạo GetBookRequest, gọi port.getBook(request), ánh xạ bằng toModel(...)
        // TODO(TH1-7): bắt BookCatalogFault, SOAPFaultException, WebServiceException
        //              và ném lại bằng translate(...) / unavailable(...)
        try {
		GetBookRequest request = new GetBookRequest();
		request.setIsbn(isbn);
		GetBookResponse response = port.getBook(request);
		return toModel(response.getBook());
	    } catch (BookCatalogFault fault) {
		throw translate(fault);
	    } catch (SOAPFaultException e) {
		throw translate(e);
	    } catch (WebServiceException e) {
		throw unavailable(e);
	    }
    }

    public List<LegacyBook> search(String keyword, int maxResults) {
        // TODO(TH1-6): tạo SearchBooksRequest (keyword, maxResults), gọi port.searchBooks(...)
		try {
		SearchBooksRequest request = new SearchBooksRequest();
		request.setKeyword(keyword);
		request.setMaxResults(maxResults);
		SearchBooksResponse response = port.searchBooks(request);
		return response.getBook().stream()
		               .map(LegacyCatalogClient::toModel)
		               .toList();
	    } catch (BookCatalogFault fault) {
		throw translate(fault);
	    } catch (SOAPFaultException e) {
		throw translate(e);
	    } catch (WebServiceException e) {
		throw unavailable(e);
	    }
    }

    /** TODO(TH1-7): BOOK_NOT_FOUND → NOT_FOUND; INVALID_ISBN/INVALID_REQUEST → INVALID_REQUEST; còn lại → UPSTREAM_ERROR. */
    static LegacyCatalogException translate(BookCatalogFault fault) {
        ErrorDetail detail = fault.getFaultInfo();
    String code = (detail != null && detail.getCode() != null) ? detail.getCode() : "UNKNOWN";
    String msg = (detail != null && detail.getMessage() != null) ? detail.getMessage() : fault.getMessage();

    LegacyCatalogException.Kind kind = switch (code) {
        case "BOOK_NOT_FOUND" -> LegacyCatalogException.Kind.NOT_FOUND;
        case "INVALID_ISBN", "INVALID_REQUEST" -> LegacyCatalogException.Kind.INVALID_REQUEST;
        default -> LegacyCatalogException.Kind.UPSTREAM_ERROR;
    };
    return new LegacyCatalogException(kind, code, msg, fault);
    }

    /** TODO(TH1-7): faultcode Client → INVALID_REQUEST, Server → UPSTREAM_ERROR. */
    static LegacyCatalogException translate(SOAPFaultException e) {
        String faultCode = e.getFault().getFaultCodeAsQName().getLocalPart();
    LegacyCatalogException.Kind kind = "Client".equalsIgnoreCase(faultCode)
            ? LegacyCatalogException.Kind.INVALID_REQUEST
            : LegacyCatalogException.Kind.UPSTREAM_ERROR;
    return new LegacyCatalogException(kind, "soap:" + faultCode, e.getMessage(), e);
    }

    /** TODO(TH1-7): phân biệt hết thời gian chờ (SocketTimeoutException) với không kết nối được. */
    static LegacyCatalogException unavailable(WebServiceException e) {
        boolean isTimeout = false;
    for (Throwable c = e; c != null; c = c.getCause()) {
        if (c instanceof java.net.SocketTimeoutException || c instanceof java.net.http.HttpTimeoutException) {
            isTimeout = true;
            break;
        }
    }
    LegacyCatalogException.Kind kind = isTimeout 
            ? LegacyCatalogException.Kind.UPSTREAM_TIMEOUT 
            : LegacyCatalogException.Kind.UPSTREAM_UNAVAILABLE;
    return new LegacyCatalogException(kind, null, e.getMessage(), e);
    }

    /** Ánh xạ kiểu SOAP → mô hình nội bộ (không để lớp sinh từ WSDL "rò" ra API REST). */
    static LegacyBook toModel(Book b) {
        Integer authorId = b.getAuthorId() == null ? null : b.getAuthorId().intValue();
        Integer year = b.getPublishedYear() == null ? null : b.getPublishedYear().getYear();
        return new LegacyBook(b.getIsbn(), b.getTitle(), b.getAuthor(), authorId, year,
                b.getPrice().getAmount(), b.getPrice().getCurrency().value());
    }
}
