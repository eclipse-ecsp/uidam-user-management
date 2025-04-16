/*
 * Copyright (c) 2023 - 2024 Harman International
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.eclipse.ecsp.uidam.usermanagement.utilities;

import java.math.BigInteger;
import java.time.LocalDate;
import java.util.UUID;

/**
 * User Test Constants.
 */
public interface Constants {

    String USER_ID = "id";
    String USER_NAME = "userName";
    String PASSWORD = "password";
    String FIRST_NAME = "firstName";
    String LAST_NAME = "lastName";
    String ROLES = "roles";
    String ADDRESS1 = "address1";
    String ADDRESS2 = "address2";
    String COUNTRY = "country";
    String STATE = "state";
    String CITY = "city";
    String POSTAL_CODE = "postalCode";
    String PHONE_NUMBER = "phoneNumber";
    String EMAIL = "email";
    String LOCALE = "locale";
    String GENDER = "gender";
    String BIRTH_DATE = "birthDate";
    String DEV_ID = "devId";

    LocalDate BIRTH_DATE_VALUE = LocalDate.of(1997, 11, 20);

    BigInteger USER_ID_VALUE = new BigInteger("145911385530649019822702644100150");
    BigInteger ATTR_ID_VALUE = new BigInteger("145941385530649019822702644100150");
    BigInteger ATTR_ID_VALUE_1 = new BigInteger("145941385530649059822702644100150");
    BigInteger ATTR_ID_VALUE_2 = new BigInteger("145941385530649019862702644100150");
    BigInteger USER_ID_VALUE_2 = new BigInteger("145911385530649019822702444100150");
    BigInteger USER_ID_VALUE_3 = new BigInteger("145914385530649019822702444100150");
    BigInteger USER_ID_VALUE_4 = new BigInteger("145913385530649019822702444100150");

    BigInteger USER_ENTITY_ID_VALUE = new BigInteger("145913385530649019824702444100150");
    BigInteger LOGGED_IN_USER_ID_VALUE = new BigInteger("45061006022041199432381286212171");
    public static final String USER_DEFAULT_ACCOUNT = "userdefaultaccount";
    BigInteger ACCOUNT_ID_VALUE = new BigInteger("145911385530649019822702644100150");
    BigInteger ACCOUNT_ID_VALUE_1 = new BigInteger("145911385530649019822702644101111");
    BigInteger ACCOUNT_ID_VALUE_2 = new BigInteger("145911385530649019822702644102222");
    BigInteger ACCOUNT_ID_VALUE_3 = new BigInteger("145911385530649019822702644103333");
    BigInteger ACCOUNT_ID_VALUE_4 = new BigInteger("14591138553064901982270264414444");
    BigInteger ACCOUNT_ID_VALUE_5 = new BigInteger("1459113855306490198227026445555");
    BigInteger ACCOUNT_ID_VALUE_6 = new BigInteger("14591138553064901982270264416666");
    BigInteger ACCOUNT_ID_VALUE_7 = new BigInteger("14591138553064901982270264417777");
    BigInteger PARENT_ID_VALUE = new BigInteger("136941385530649019822702644100150");
    BigInteger PARENT_ID_VALUE_1 = new BigInteger("111111385530649019822702644100150");

    BigInteger PARENT_ID_VALUE_2 = new BigInteger("222222385530649019822702644100150");
    BigInteger PARENT_ID_VALUE_3 = new BigInteger("333333385530649019822702644100150");
    BigInteger PARENT_ID_VALUE_4 = new BigInteger("444422385530649019822702644100150");
    BigInteger PARENT_ID_VALUE_5 = new BigInteger("555552385530649019822702644100150");
    BigInteger PARENT_ID_VALUE_6 = new BigInteger("66666385530649019822702644100150");
    BigInteger PARENT_ID_VALUE_7 = new BigInteger("777777385530649019822702644100150");
    BigInteger PARENT_ID_VALUE_8 = new BigInteger("88888385530649019822702644100150");
    BigInteger CREATED_BY_ID_VALUE = new BigInteger("124941385530649059822702644100150");

    String ANOTHER_USER_ID_VALUE = String.valueOf(UUID.randomUUID());
    String FIRST_NAME_VALUE = "John";
    String MODIFIED_FIRST_NAME = "Bon";
    String MODIFIED_CITY = "Delhi";
    String LAST_NAME_VALUE = "Doe";
    String ROLE_VALUE = "VEHICLE_OWNER";
    String INVALID_ROLE_VALUE = "VEHICLE_OWNER1";
    String ADMIN_ROLE = "BUSINESS_ADMIN";

    String SCOPE_SELF_MNG = "SelfManage";
    String ROLE_2 = "ROLE_2";
    String ADDRESS1_VALUE = "5801 S Ellis Ave";
    String ADDRESS2_VALUE = "Some address line 2";
    String COUNTRY_VALUE = "USA";
    String STATE_VALUE = "Illinois";
    String CITY_VALUE = "Chicago";
    String POSTAL_CODE_VALUE = "12121";
    String PHONE_NUMBER_VALUE = "+17535011234";
    String LOCALE_VALUE = "en_US";
    String EMAIL_VALUE = "john.doe@email.com";
    String USER_NAME_VALUE = "johnd";
    String ANOTHER_USER_NAME = "mariam";
    String FEDERATED_PREFIX = "federated_";
    String PASSWORD_VALUE = "April@12q34";
    String ANOTHER_PASSWORD = "Bb1234";
    String DEV_ID_VALUE = "devId321";
    String ANOTHER_DEV_ID = "devId_other";
    String IDENTITY_PROVIDER_VALUE = "some-company";
    String EXTERNAL_USER_NAME_VALUE = "johnd@some-company.com";


    String CONSUMER_1 = "Consumer.1.";
    String CONSUMER_2 = "Consumer.2.";

    String WSO2_ID = "WSO2_ID";
    String ANOTHER_WSO2_ID = "ANOTHER_WSO2_ID";
    String SESSION = "session";
    String SERVER_TOMCAT_MAX_THREAD = "server.tomcat.max-threads=5";
    String LOG_LEVEL = "log.level=warn";
    String IS_EMAIL_VERIFICATION_ENABLED = "IS_EMAIL_VERIFICATION_ENABLED=false";
    String PROFILE_TEST = "test";
    String SPRING_MAIL_HOST = "spring.mail.host=testHost";
    String SPRING_MAIL_PORT = "spring.mail.port=3000";
    String SPRING_MAIL_PASSWORD = "spring.mail.password=dolphins";
    String SPRING_MAIL_USERNAME = "spring.mail.username=testUser";
    int MIN_PASSWORD_LENGTH = 10;
    int MAX_PASSWORD_LENGTH = 15;
    String PASSWORD_ENCODER = "SHA256";
    long RECOVERY_SECRET_EXPIRE_IN_MINUTES = 1;
    long INTERVAL_FOR_LAST_PASSWORD_UPDATE = 0L;

    BigInteger ROLE_ID_1 = new BigInteger("145911385590649014822702644100150");
    BigInteger ROLE_ID_2 = new BigInteger("145911385590649014822702644100151");
    BigInteger ROLE_ID_3 = new BigInteger("145911385590649014822702644100152");
    BigInteger ROLE_ID_4 = new BigInteger("145911385590649014822702644100153");
    BigInteger ROLE_ID_5 = new BigInteger("145911385590649014822702644100154");
    BigInteger ROLE_ID_6 = new BigInteger("145911385590649014822702644100155");

    BigInteger SCOPE_ID = new BigInteger("145911385590649014822702644100150");
}
