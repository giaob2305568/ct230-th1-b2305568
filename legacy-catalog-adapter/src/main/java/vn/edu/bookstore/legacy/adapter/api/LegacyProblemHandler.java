package vn.edu.bookstore.legacy.adapter.api;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.ResponseEntity;
import vn.edu.bookstore.legacy.adapter.soap.LegacyCatalogException;

/** Chuyển lỗi → Problem Details (RFC 9457). */
@RestControllerAdvice
public class LegacyProblemHandler {

    // TODO(TH1-7): thêm @ExceptionHandler(LegacyCatalogException.class) ánh xạ:
    //   NOT_FOUND → 404 book-not-found; INVALID_REQUEST → 400 validation-failed;
    //   UPSTREAM_ERROR → 502, UPSTREAM_TIMEOUT → 504, UPSTREAM_UNAVAILABLE → 503 (upstream-unavailable);
    //   thêm thuộc tính "legacyCode" và instance = đường dẫn yêu cầu.
	
@ExceptionHandler(LegacyCatalogException.class)
public ResponseEntity<ProblemDetail> handleLegacyCatalogException(LegacyCatalogException e, HttpServletRequest request) {
    HttpStatus status = switch (e.kind()) {
        case NOT_FOUND -> HttpStatus.NOT_FOUND;
        case INVALID_REQUEST -> HttpStatus.BAD_REQUEST;
        case UPSTREAM_ERROR -> HttpStatus.BAD_GATEWAY;
        case UPSTREAM_TIMEOUT -> HttpStatus.GATEWAY_TIMEOUT;
        case UPSTREAM_UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE;
    };

    URI typeUri = switch (e.kind()) {
        case NOT_FOUND -> ProblemTypes.BOOK_NOT_FOUND;
        case INVALID_REQUEST -> ProblemTypes.VALIDATION_FAILED;
        default -> ProblemTypes.UPSTREAM_UNAVAILABLE;
    };

    ProblemDetail p = problem(status, typeUri, status.getReasonPhrase(), e.getMessage());
    if (e.legacyCode() != null) {
        p.setProperty("legacyCode", e.legacyCode());
    }
    p.setInstance(URI.create(request.getRequestURI()));

    return ResponseEntity.status(status).body(p);
}

    @ExceptionHandler(InvalidRequestException.class)
    ProblemDetail onInvalid(InvalidRequestException e, HttpServletRequest request) {
        ProblemDetail p = problem(HttpStatus.BAD_REQUEST, ProblemTypes.VALIDATION_FAILED,
                "Yêu cầu không hợp lệ", e.getMessage());
        p.setInstance(URI.create(request.getRequestURI()));
        return p;
    }

    private static ProblemDetail problem(HttpStatus status, URI type, String title, String detail) {
        ProblemDetail p = ProblemDetail.forStatusAndDetail(status, detail);
        p.setType(type);
        p.setTitle(title);
        return p;
    }
}
