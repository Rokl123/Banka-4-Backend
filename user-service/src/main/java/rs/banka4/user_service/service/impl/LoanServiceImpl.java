package rs.banka4.user_service.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import rs.banka4.user_service.domain.loan.db.LoanStatus;
import rs.banka4.user_service.domain.loan.db.Loan;
import rs.banka4.user_service.domain.account.db.Account;
import rs.banka4.user_service.domain.account.dtos.AccountDto;
import rs.banka4.user_service.domain.loan.dtos.LoanApplicationDto;
import rs.banka4.user_service.domain.loan.dtos.LoanFilterDto;
import rs.banka4.user_service.domain.loan.dtos.LoanInformationDto;
import rs.banka4.user_service.domain.user.client.db.Client;
import rs.banka4.user_service.exceptions.NullPageRequest;
import rs.banka4.user_service.repositories.LoanRepository;
import rs.banka4.user_service.utils.loans.LoanRateUtil;
import rs.banka4.user_service.domain.loan.mapper.LoanMapper;
import rs.banka4.user_service.exceptions.account.AccountNotActive;
import rs.banka4.user_service.exceptions.account.AccountNotFound;
import rs.banka4.user_service.exceptions.user.NotFound;
import rs.banka4.user_service.exceptions.user.client.ClientNotFound;
import rs.banka4.user_service.service.abstraction.AccountService;
import rs.banka4.user_service.service.abstraction.ClientService;
import rs.banka4.user_service.exceptions.jwt.Unauthorized;
import rs.banka4.user_service.exceptions.loan.InvalidLoanStatus;
import rs.banka4.user_service.exceptions.loan.LoanNotFound;
import rs.banka4.user_service.domain.loan.specification.LoanSpecification;
import rs.banka4.user_service.service.abstraction.LoanService;
import rs.banka4.user_service.utils.JwtUtil;
import rs.banka4.user_service.utils.specification.SpecificationCombinator;


import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
@Primary
public class LoanServiceImpl implements LoanService {

    private final ClientService clientService;

    private final AccountService accountService;

    private final LoanRateUtil loanRateUtil;
    private final LoanRepository loanRepository;

    private final JwtUtil jwtUtil;


    @Transactional
    @Override
    public void createLoanApplication(LoanApplicationDto loanApplicationDto, String auth) {

        String email = jwtUtil.extractUsername(auth);

        Optional<Client> client = clientService.getClientByEmail(email);

        if(client.isEmpty())
            throw new ClientNotFound(email);

        Loan newLoan = LoanMapper.INSTANCE.toEntity(loanApplicationDto);

        newLoan.setStatus(LoanStatus.PROCESSING);

        newLoan.setRemainingDebt(loanApplicationDto.amount());

        connectAccountToLoan(loanApplicationDto,newLoan);

        generateAccountNumber(newLoan);

        // TODO: Calculate the monthly installment for the loan

        generateLoanNumber(newLoan);
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

        if (pageRequest == null) {
            throw new NullPageRequest();
        }

        String username = jwtUtil.extractUsername(token);

        Optional<Client> client = clientService.getClientByEmail(username);

        if(client.isEmpty())
            throw new ClientNotFound(username);

        Set<AccountDto> accounts = accountService.getAccountsForClient(token);

        if(accounts.isEmpty())
            throw new NoLoansOnAccount(username);

        Set<String> accountNumbers = accounts.stream().map(AccountDto::accountNumber).collect(Collectors.toSet());

        SpecificationCombinator<Loan> combinator = new SpecificationCombinator<>();

        for(String accountNumber : accountNumbers) {
            combinator.or(LoanSpecification.hasAccountNumber(accountNumber));
        }

        Sort sort = Sort.by(Sort.Direction.DESC, "amount");

        PageRequest pageRequestWithSort = PageRequest.of(pageRequest.getPageNumber(),
                pageRequest.getPageSize(),
                sort);

        Page<Loan> loansPage = loanRepository.findAll(combinator.build(),pageRequestWithSort);

        Page<LoanInformationDto> loanDtoPage = loansPage.map(LoanMapper.INSTANCE::toDto);

        return ResponseEntity.ok(loanDtoPage);
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
        try {
            Account account =  accountService.getAccountByAccountNumber(loanApplicationDto.accountNumber());

            if(!account.isActive())
                throw new AccountNotActive();

            newLoan.setAccount(account);

            account.setBalance(account.getBalance().add(newLoan.getAmount()));
        }catch (NotFound nf){
            throw new AccountNotFound(loanApplicationDto.accountNumber());
        }
    }

    private void generateLoanNumber(Loan newLoan){
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

                loanRepository.save(newLoan);

                break;
            }catch (DataIntegrityViolationException e){
                System.out.println("Loan with this loan number already exists!");
            }
        }
    }
}
