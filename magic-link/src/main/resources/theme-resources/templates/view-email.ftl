<#import "template.ftl" as layout>
<@layout.registrationLayout displayInfo=social.displayInfo; section>
    <#if section = "title">
        ${msg("loginTitle",(realm.displayName!''))}
    <#elseif section = "header">
        ${msg("loginTitleHtml",(realm.displayNameHtml!''))?no_esc}
    <#elseif section = "form">
        <#if realm.password>
        <div>
            Please, read your email inbox and follow the instruction
        </div>
            ${auth.attemptedUsername}
        </#if>
    </#if>
</@layout.registrationLayout>
