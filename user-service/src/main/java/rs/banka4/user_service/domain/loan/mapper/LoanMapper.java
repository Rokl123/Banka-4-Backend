package rs.banka4.user_service.domain.loan.mapper;

import org.mapstruct.*;
import org.mapstruct.factory.Mappers;
import rs.banka4.user_service.domain.account.mapper.AccountMapper;
import rs.banka4.user_service.domain.currency.dtos.CurrencyDto;
import rs.banka4.user_service.domain.currency.mapper.CurrencyMapper;
import rs.banka4.user_service.domain.loan.db.Loan;
import rs.banka4.user_service.domain.loan.db.LoanRequest;
import rs.banka4.user_service.domain.loan.dtos.LoanApplicationDto;
import rs.banka4.user_service.domain.loan.dtos.LoanInformationDto;
import rs.banka4.user_service.domain.user.employee.db.Employee;
import rs.banka4.user_service.domain.user.employee.dtos.CreateEmployeeDto;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE,
uses = {CurrencyMapper.class, AccountMapper.class})
public interface LoanMapper {

    LoanMapper INSTANCE = Mappers.getMapper(LoanMapper.class);

    LoanInformationDto toDto(Loan loan);

//    @AfterMapping
//    default void mapCurrency(Loan loan, @MappingTarget LoanInformationDto loanInformationDto) {
//        if (loan.getAccount() != null) {
//            loanInformationDto.currency() = CurrencyMapper.INSTANCE.toDto(loan.getAccount().getCurrency());
//        }
//    }

    @Mapping(source = "loanType",target = "type")
    Loan toEntity(LoanApplicationDto loanApplicationDto);

    @Mapping(target = "currency",ignore = true)
    @Mapping(target = "type", source = "loanType")
    LoanRequest toLoanRequest(LoanApplicationDto loanApplicationDto);

}
