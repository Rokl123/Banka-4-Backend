package rs.banka4.user_service.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.banka4.user_service.domain.loan.db.LoanStatus;
import rs.banka4.user_service.domain.loan.db.Loan;
import rs.banka4.user_service.domain.loan.dtos.LoanApplicationDto;
import rs.banka4.user_service.domain.loan.dtos.LoanFilterDto;
import rs.banka4.user_service.domain.loan.dtos.LoanInformationDto;
import rs.banka4.user_service.repositories.LoanRepository;
import rs.banka4.user_service.service.abstraction.AccountService;
import rs.banka4.user_service.exceptions.jwt.Unauthorized;
import rs.banka4.user_service.exceptions.loan.InvalidLoanStatus;
import rs.banka4.user_service.exceptions.loan.LoanNotFound;
import rs.banka4.user_service.domain.loan.mapper.LoanMapper;
import rs.banka4.user_service.domain.loan.specification.LoanSpecification;
import rs.banka4.user_service.service.abstraction.LoanService;
import rs.banka4.user_service.utils.JwtUtil;
import java.time.LocalDate;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Primary
public class LoanServiceImpl implements LoanService {

    private final AccountService accountService;

    private final LoanRepository loanRepository;

    private final JwtUtil jwtUtil;


    @Transactional
    @Override
    public void createLoanApplication(LoanApplicationDto loanApplicationDto, String auth) {

//        String email = jwtUtil.extractUsername(auth);
//
//        Optional<Client> client = clientService.getClientByEmail(email);
//
//        if(client.isEmpty())
//            throw new ClientNotFound(email);

        Loan newLoan = LoanMapper.INSTANCE.toEntity(loanApplicationDto);

        newLoan.setStatus(LoanStatus.PROCESSING);

        newLoan.setNextInstallmentDate(LocalDate.now().plusMonths(1));

        newLoan.setRemainingDebt(loanApplicationDto.amount());

        connectAccountToLoan(loanApplicationDto,newLoan);

        generateAccountNumber(newLoan);

        // TODO: Calculate the monthly installment for the loan

        loanRepository.save(newLoan);
    }



    @Override
    public ResponseEntity<Page<LoanInformationDto>> getAllLoans(PageRequest pageRequest, LoanFilterDto filterDto) {
        Sort sort = Optional.ofNullable(filterDto.status())
                .filter(status -> status.equals(LoanStatus.PROCESSING))
                .map(status -> Sort.by("agreementDate").descending())
                .orElse(Sort.by("account.accountNumber"));

        Page<Loan> loanPage = loanRepository.findAll(LoanSpecification.searchLoans(filterDto), pageRequest.withSort(sort));

        return ResponseEntity.ok(loanPage.map(LoanMapper.INSTANCE::toDto));
    }

    @Override
    public ResponseEntity<Page<LoanInformationDto>> getMyLoans(String token, PageRequest pageRequest) {
        return null;
        // check out /client/search
    }

    @Transactional
    @Override
    public void approveLoan(Long loanNumber, String auth) {
        var role = jwtUtil.extractRole(auth);
        if (!role.equals("employee"))
            throw new Unauthorized(auth);

        var loan = loanRepository.findByLoanNumber(loanNumber);

        if (loan.isEmpty())
            throw new LoanNotFound();

        if(!loan.get().getStatus().equals(LoanStatus.PROCESSING))
            throw new InvalidLoanStatus(loan.get().getStatus().name());

        loan.get().setNextInstallmentDate(LocalDate.now().plusMonths(1));
        loan.get().setDueDate(LocalDate.now().plusMonths(loan.get().getRepaymentPeriod()));
        loan.get().setStatus(LoanStatus.APPROVED);
        loan.get().setAgreementDate(LocalDate.now());

        loanRepository.save(loan.get());
    }

    @Transactional
    @Override
    public void rejectLoan(Long loanNumber, String auth) {
        var role = jwtUtil.extractRole(auth);
        if (!role.equals("employee"))
            throw new Unauthorized(auth);

        var loan = loanRepository.findByLoanNumber(loanNumber);

        if (loan.isEmpty())
            throw new LoanNotFound();

        if(!loan.get().getStatus().equals(LoanStatus.PROCESSING))
            throw new InvalidLoanStatus(loan.get().getStatus().name());

        loan.get().setStatus(LoanStatus.REJECTED);

        loanRepository.save(loan.get());

    }

    private void connectAccountToLoan(LoanApplicationDto loanApplicationDto, Loan newLoan) {

    }

    private void generateAccountNumber(Loan newLoan){
        String comb;

        LocalDate dayOfLoanRequest = LocalDate.now();

        if(dayOfLoanRequest.getMonthValue() < 10)
             comb = dayOfLoanRequest.getYear() + "0" + dayOfLoanRequest.getMonthValue();
        else
            comb = dayOfLoanRequest.getYear() + "" + dayOfLoanRequest.getMonthValue();

        while(true){
            try{
                long random = ThreadLocalRandom.current().nextLong(0,100000);

                String loanNumber = String.format("%06d",Integer.parseInt(comb) + random);

                newLoan.setLoanNumber(Long.valueOf(loanNumber));

                break;
            }catch (DataIntegrityViolationException e){
                System.out.println("Loan with this loan number already exists!");
            }
        }
    }
}
