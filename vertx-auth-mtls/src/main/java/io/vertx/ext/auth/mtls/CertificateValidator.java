/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.vertx.ext.auth.mtls;

import io.vertx.ext.auth.authentication.CertificateCredentials;
import io.vertx.ext.auth.authentication.CredentialValidationException;

/**
 * Interface for validating certificates for mutual TLS authentication.
 * <p>
 * This interface can be implemented to provide logic for validating various fields from Certificates.
 */
public interface CertificateValidator
{
    /**
     * Validates if certificates shared as part of {@link CertificateCredentials} are valid.
     *
     * <p>For example:
     * <ul>
     *  <li>Verifying CA information
     *  <li>Checking CN information
     *  <li>Validating Issuer information
     *  <li>Checking organization information etc
     * </ul>
     *
     * @param credentials user certificate credentials shared
     * @throws CredentialValidationException when certificate is not valid.
     */
    void verifyCertificate(CertificateCredentials credentials) throws CredentialValidationException;
}
