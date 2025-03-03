package rs.banka4.user_service.service.abstraction;

import org.springframework.http.ResponseEntity;
import rs.banka4.user_service.dto.requests.CreateCompanyDto;

public interface CompanyService {
    ResponseEntity<Void> creteCompany(CreateCompanyDto dto);
}
