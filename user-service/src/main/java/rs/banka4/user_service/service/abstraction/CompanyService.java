package rs.banka4.user_service.service.abstraction;

import org.springframework.http.ResponseEntity;
import rs.banka4.user_service.dto.requests.CreateCompanyDto;
import rs.banka4.user_service.models.Company;

import java.util.Optional;
import java.util.UUID;

public interface CompanyService {
    ResponseEntity<Void> creteCompany(CreateCompanyDto dto);

    Optional<Company> getCompany(String id);

    Optional<Company> getCompanyByCrn(String crn);
}
