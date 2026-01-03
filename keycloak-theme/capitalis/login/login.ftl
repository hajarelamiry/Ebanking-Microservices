<#import "template.ftl" as layout>
<@layout.registrationLayout displayInfo=social.displayInfo; section>
    <#if section = "header">
        <div class="header-container">
            <h1 id="kc-page-title">Sign in to Capitalis</h1>
            <p class="subtitle">Access your financial dashboard in seconds.</p>
        </div>
    <#elseif section = "form">
    <div id="kc-form">
      <div id="kc-form-wrapper">
        <form id="kc-form-login" onsubmit="login.disabled = true; return true;" action="${url.loginAction}" method="post">
            <div class="form-group mb-4">
                <label for="username" class="form-label"><#if !realm.loginWithEmailAllowed>${msg("username")}<#elseif !realm.registrationEmailAsUsername>${msg("usernameOrEmail")}<#else>${msg("email")}</#if></label>
                <input tabindex="1" id="username" class="form-control" name="username" value="${(login.username!'')}"  type="text" autofocus autocomplete="off" />
            </div>

            <div class="form-group mb-4">
                <label for="password" class="form-label">${msg("password")}</label>
                <input tabindex="2" id="password" class="form-control" name="password" type="password" autocomplete="off" />
            </div>

            <div id="kc-form-options" class="mb-4 d-flex justify-content-between align-items-center">
                <#if realm.rememberMe && !login.rememberMeAllowed??>
                    <div class="checkbox">
                        <label>
                            <#if login.rememberMe??>
                                <input tabindex="3" id="rememberMe" name="rememberMe" type="checkbox" checked> ${msg("rememberMe")}
                            <#else>
                                <input tabindex="3" id="rememberMe" name="rememberMe" type="checkbox"> ${msg("rememberMe")}
                            </#if>
                        </label>
                    </div>
                </#if>
                <div class="${properties.kcFormOptionsWrapperClass!}">
                    <#if realm.resetPasswordAllowed>
                        <span><a tabindex="5" href="${url.loginResetCredentialsUrl}" class="forgot-password">${msg("doForgotPassword")}</a></span>
                    </#if>
                </div>
            </div>

            <div id="kc-form-buttons" class="form-group">
                <input tabindex="4" class="btn btn-primary btn-block btn-lg" name="login" id="kc-login" type="submit" value="${msg("doLogIn")}"/>
            </div>
        </form>
      </div>
    </div>
    <#elseif section = "info" >
        <#if realm.password && realm.registrationAllowed && !registrationDisabled??>
            <div id="kc-registration">
                <span>${msg("noAccount")} <a tabindex="6" href="${url.registrationUrl}">${msg("doRegister")}</a></span>
            </div>
        </#if>
    </#if>
</@layout.registrationLayout>
