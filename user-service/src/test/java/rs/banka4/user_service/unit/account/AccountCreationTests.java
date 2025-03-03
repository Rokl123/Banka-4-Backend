package rs.banka4.user_service.unit.account;

import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import rs.banka4.user_service.mapper.ClientMapper;
import rs.banka4.user_service.mapper.CompanyMapper;
import rs.banka4.user_service.mapper.EmployeeMapper;
import rs.banka4.user_service.repositories.CurrencyRepository;
import rs.banka4.user_service.service.abstraction.AccountService;
import rs.banka4.user_service.service.abstraction.ClientService;
import rs.banka4.user_service.service.abstraction.CompanyService;
import rs.banka4.user_service.service.abstraction.EmployeeService;

public class AccountCreationTests {

    @Mock
    private ClientService clientService;

    @Mock
    private CompanyService companyService;

    @Mock
    private ClientMapper clientMapper;

    @Mock
    private CurrencyRepository currencyRepository;

    @Mock
    private CompanyMapper companyMapper;

    @Mock
    private EmployeeService employeeService;

    @Mock
    private EmployeeMapper employeeMapper;

    @InjectMocks
    private AccountService accountService;

    @Test
    public void testAccountCreation() {

    }
}
