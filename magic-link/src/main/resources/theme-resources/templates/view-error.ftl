<#import "template.ftl" as layout>
<@layout.registrationLayout displayInfo=social.displayInfo; section>
    <#if section = "title">
        ${msg("loginTitle",(realm.displayName!''))}
    <#elseif section = "header">
        ${msg("loginTitleHtml",(realm.displayNameHtml!''))?no_esc}
    <#elseif section = "form">
        <#if realm.password>
          <div>
              Error : The email submitted is not valid. 
              <br/>
              The email is not valid. 
              Please, make sure that the email is correct or contact support
          </div>
          <a id="reset-login" href="${url.loginRestartFlowUrl}">
              <div class="kc-login-tooltip">
                  <i class="${properties.kcResetFlowIcon!}"></i>
                  <span class="kc-tooltip-text">${msg("restartLoginTooltip")}</span>
              </div>
          </a>
            
        </#if>
    </#if>
</@layout.registrationLayout>
