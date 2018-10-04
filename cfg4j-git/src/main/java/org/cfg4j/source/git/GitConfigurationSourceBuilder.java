/*
 * Copyright 2015-2018 Norbert Potocki (norbert.potocki@nort.pl)
 *
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
 */
package org.cfg4j.source.git;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;
import org.cfg4j.source.context.filesprovider.ConfigFilesProvider;
import org.cfg4j.source.context.filesprovider.DefaultConfigFilesProvider;
import org.cfg4j.source.context.propertiesprovider.JsonBasedPropertiesProvider;
import org.cfg4j.source.context.propertiesprovider.PropertiesProviderSelector;
import org.cfg4j.source.context.propertiesprovider.PropertyBasedPropertiesProvider;
import org.cfg4j.source.context.propertiesprovider.YamlBasedPropertiesProvider;
import org.eclipse.jgit.api.TransportConfigCallback;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.JschConfigSessionFactory;
import org.eclipse.jgit.transport.OpenSshConfig.Host;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.eclipse.jgit.transport.SshTransport;
import org.eclipse.jgit.transport.Transport;
import org.eclipse.jgit.util.FS;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.StringJoiner;

/**
 * Builder for {@link GitConfigurationSource}.
 */
public class GitConfigurationSourceBuilder {

  private BranchResolver branchResolver;
  private PathResolver pathResolver;
  private String repositoryURI;
  private Path tmpPath;
  private String tmpRepoPrefix;
  private ConfigFilesProvider configFilesProvider;
  private PropertiesProviderSelector propertiesProviderSelector;
  private CredentialsProvider credentialsProvider;
  private TransportConfigCallback transportConfigCallback;

  /**
   * Construct {@link GitConfigurationSource}s builder
   * <p>
   * Default setup (override using with*() methods)
   * <ul>
   * <li>BranchResolver: {@link FirstTokenBranchResolver}</li>
   * <li>PathResolver: {@link AllButFirstTokenPathResolver}</li>
   * <li>ConfigFilesProvider: {@link DefaultConfigFilesProvider}</li>
   * <li>tmpPath: System.getProperty("java.io.tmpdir")</li>
   * <li>tmpRepoPrefix: "cfg4j-config-git-config-repository"</li>
   * <li>propertiesProviderSelector: {@link PropertiesProviderSelector} with {@link PropertyBasedPropertiesProvider}
   * and {@link YamlBasedPropertiesProvider} providers</li>
   * </ul>
   */
  public GitConfigurationSourceBuilder() {
    branchResolver = new FirstTokenBranchResolver();
    pathResolver = new AllButFirstTokenPathResolver();
    tmpPath = Paths.get(System.getProperty("java.io.tmpdir"));
    tmpRepoPrefix = "cfg4j-git-config-repository";
    configFilesProvider = new DefaultConfigFilesProvider();
    propertiesProviderSelector = new PropertiesProviderSelector(
      new PropertyBasedPropertiesProvider(),
      new YamlBasedPropertiesProvider(),
      new JsonBasedPropertiesProvider()
    );
  }

  /**
   * Set {@link BranchResolver} for {@link GitConfigurationSource}s built by this builder
   *
   * @param branchResolver {@link BranchResolver} to use
   * @return this builder with {@link BranchResolver} set to {@code branchResolver}
   */
  public GitConfigurationSourceBuilder withBranchResolver(BranchResolver branchResolver) {
    this.branchResolver = branchResolver;
    return this;
  }

  /**
   * Set {@link PathResolver} for {@link GitConfigurationSource}s built by this builder
   *
   * @param pathResolver {@link PathResolver} to use
   * @return this builder with {@link PathResolver} set to {@code pathResolver}
   */
  public GitConfigurationSourceBuilder withPathResolver(PathResolver pathResolver) {
    this.pathResolver = pathResolver;
    return this;
  }

  /**
   * Set repository location for {@link GitConfigurationSource}s built by this builder
   *
   * @param repositoryURI repository location to use
   * @return this builder with repository location set to {@code repositoryURI}
   */
  public GitConfigurationSourceBuilder withRepositoryURI(String repositoryURI) {
    this.repositoryURI = repositoryURI;
    return this;
  }

  /**
   * Set temporary dir path for {@link GitConfigurationSource}s built by this builder
   *
   * @param tmpPath temporary dir path to use
   * @return this builder with temporary dir path set to {@code tmpPath}
   */
  public GitConfigurationSourceBuilder withTmpPath(Path tmpPath) {
    this.tmpPath = tmpPath;
    return this;
  }

  /**
   * Set relative repository path in temporary dir for {@link GitConfigurationSource}s built by this builder
   *
   * @param tmpRepoPrefix relative repository path in temporary dir to use
   * @return this builder with relative repository path in temporary dir set to {@code tmpRepoPrefix}
   */
  public GitConfigurationSourceBuilder withTmpRepoPrefix(String tmpRepoPrefix) {
    this.tmpRepoPrefix = tmpRepoPrefix;
    return this;
  }

  /**
   * Set {@link ConfigFilesProvider} for {@link GitConfigurationSource}s built by this builder
   *
   * @param configFilesProvider {@link ConfigFilesProvider} to use
   * @return this builder with {@link ConfigFilesProvider} set to {@code configFilesProvider}
   */
  public GitConfigurationSourceBuilder withConfigFilesProvider(ConfigFilesProvider configFilesProvider) {
    this.configFilesProvider = configFilesProvider;
    return this;
  }

  /**
   * Set {@link CredentialsProvider} for {@link GitConfigurationSource} built by this builder
   *
   * @param credentialsProvider {@link CredentialsProvider} to use
   * @return this builder after {@code credentialsProvider} is set
   */
  public GitConfigurationSourceBuilder withCredentialsProvider(CredentialsProvider credentialsProvider) {
    this.credentialsProvider = credentialsProvider;
    return this;
  }

  /**
   * When using with ssh transport, use {@code ~/.ssh/id_rsa} for auth.
   *
   * @return this builder with ssh transport set
   */
  public GitConfigurationSourceBuilder withDefaultSshKeys() {
    this.transportConfigCallback = transport -> ((SshTransport) transport).setSshSessionFactory(
      new JschConfigSessionFactory() {
        @Override
        protected void configure(Host hc, Session session) {
          // set StrictHostKeyChecking to no, otherwise will get org.eclipse.jgit.api.errors.transportexception UnknownHostKey error
          session.setConfig("StrictHostKeyChecking", "no");
          session.setUserInfo(new UserInfo() {
            @Override
            public String getPassphrase() {
              return "<your pass phrase>"; // if you don't have passphrase, return null
            }

            @Override
            public String getPassword() {
              return null;
            }

            @Override
            public boolean promptPassword(String s) {
              return false;
            }

            @Override
            public boolean promptPassphrase(String s) {
              return true; // if you don't have passphrase set, return false
            }

            @Override
            public boolean promptYesNo(String s) {
              return false;
            }

            @Override
            public void showMessage(String s) {

            }
          });
        }

        @Override
        protected JSch getJSch(Host hc, FS fs) throws JSchException {
          String userHome = System.getProperty("user.home");
          String privateKeyPath = userHome + "/.ssh/id_rsa";
          String knownHostsPath = userHome + "/.ssh/known_hosts";
          JSch jSch = super.getJSch(hc, fs);
          jSch.removeAllIdentity();
          jSch.addIdentity(privateKeyPath);
          jSch.setKnownHosts(knownHostsPath);
          return jSch;
        }
      });

    return this;
  }

  /**
   * Build a {@link GitConfigurationSource} using this builder's configuration
   *
   * @return new {@link GitConfigurationSource}
   */
  public GitConfigurationSource build() {
    return new GitConfigurationSource(repositoryURI,
      tmpPath,
      tmpRepoPrefix,
      branchResolver,
      pathResolver,
      configFilesProvider,
      propertiesProviderSelector,
      credentialsProvider,
      transportConfigCallback);
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", GitConfigurationSourceBuilder.class.getSimpleName() + "[",
      "]")
      .add("branchResolver=" + branchResolver)
      .add("pathResolver=" + pathResolver)
      .add("repositoryURI='" + repositoryURI + "'")
      .add("tmpPath=" + tmpPath)
      .add("tmpRepoPrefix='" + tmpRepoPrefix + "'")
      .add("configFilesProvider=" + configFilesProvider)
      .add("propertiesProviderSelector=" + propertiesProviderSelector)
      .add("credentialsProvider=" + credentialsProvider)
      .add("transportConfigCallback=" + transportConfigCallback)
      .toString();
  }
}
