package at.tuwien.ds.madmp.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class MaDmpException extends RuntimeException {
        public MaDmpException() {
            super();
        }
        public MaDmpException(String message, Throwable cause) {
            super(message, cause);
        }
        public MaDmpException(String message) {
            super(message);
        }
        public MaDmpException(Throwable cause) {
            super(cause);
        }

}
