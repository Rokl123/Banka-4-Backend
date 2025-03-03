package rs.banka4.user_service.service.impl;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import rs.banka4.user_service.dto.requests.CreateCompanyDto;
import rs.banka4.user_service.exceptions.DuplicateCrn;
import rs.banka4.user_service.exceptions.DuplicateTin;
import rs.banka4.user_service.mapper.CompanyMapper;
import rs.banka4.user_service.repositories.CompanyRepository;
import rs.banka4.user_service.service.abstraction.CompanyService;

@Service
@RequiredArgsConstructor
public class CompanyServiceImpl implements CompanyService {
    private final CompanyMapper companyMapper;
    private final CompanyRepository companyRepository;
    @Override
    public ResponseEntity<Void> creteCompany(@Valid CreateCompanyDto dto) {
        if(companyRepository.existsByCrn(dto.crn()))
            throw new DuplicateCrn(dto.crn());
        if(companyRepository.existsByTin(dto.tin()))
            throw new DuplicateTin(dto.tin());

        var comp = companyMapper.toEntity(dto);

        companyRepository.save(comp);

        return  ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
