package it.itsincom.webdevd;

import io.quarkus.qute.Template;
import it.itsincom.webdevd.model.User;
import it.itsincom.webdevd.model.Visit;
import it.itsincom.webdevd.service.CookiesSessionManager;
import it.itsincom.webdevd.service.UsersManager;
import it.itsincom.webdevd.service.VisitsManager;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;

import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Path("/reception")
public class ReceptionResource {
    private final Template reception;
    private final VisitsManager visitsManager;
    private final Template login;
    private final CookiesSessionManager cookiesSessionManager;
    private final UsersManager usersManager;

    public ReceptionResource(Template reception, VisitsManager visitsManager, Template login, CookiesSessionManager cookiesSessionManager, UsersManager usersManager) {
        this.reception = reception;
        this.visitsManager = visitsManager;
        this.login = login;
        this.cookiesSessionManager = cookiesSessionManager;
        this.usersManager = usersManager;
    }

    @GET
    public Response getReceptionPage(@CookieParam(CookiesSessionManager.COOKIE_SESSION) String sessionId) {
        System.out.println("Session ID: " + sessionId);

        if (sessionId == null || sessionId.isEmpty()) {
            System.out.println("Redirecting to login page");
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(login.data("message", "Unauthorized access. Please login.")
                            .data("redirect", true))
                    .build();
        }

        String fiscalCode = cookiesSessionManager.getUserFromSession(sessionId).getFiscalCode();
        String nameUser = cookiesSessionManager.getUserFromSession(sessionId).getNameSurname();
        System.out.println("User fiscal code: " + fiscalCode);

        List<Visit> visits = VisitsManager.getAllVisits();
        List<User> users = UsersManager.getAllEmployees();

        LocalDate today = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        today.format(formatter);

        visits.sort(Comparator.comparing(Visit::getDate).thenComparing(Visit::getStartTime).reversed());
        return Response.ok(reception.data("visit", visits, "today", today, "nome", nameUser, "employees", users)).build();
    }

    @GET
    @Path("/sort")
    public Response sortVisit(@QueryParam("dateSort") String dateSort, @CookieParam(CookiesSessionManager.COOKIE_SESSION) String sessionId) {
        List<Visit> visits = VisitsManager.getAllVisits();
        List<User> users = UsersManager.getAllEmployees();

        LocalDate today = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        today.format(formatter);

        String nameUser = cookiesSessionManager.getUserFromSession(sessionId).getNameSurname();

        if (dateSort == null) {
            visits.sort(Comparator.comparing(Visit::getDate).thenComparing(Visit::getStartTime).reversed());
            visits.removeLast();
            System.out.println(visits);
            return Response.ok(reception.data("visit", visits, "today", today, "nome", nameUser, "employees", users)).build();
        }

        LocalDate datePr = LocalDate.parse(dateSort);
        List<Visit> visitsWithSpecificDate = visits.stream().filter(v -> v.getLocalDate().isEqual(datePr)).toList();

        return Response.ok(reception.data("visit", visitsWithSpecificDate, "today", today, "nome", nameUser, "employees", users)).build();
    }

    @POST
    @Path("/logout")
    public Response logout(@CookieParam(CookiesSessionManager.COOKIE_SESSION) String sessionId) {
        if (sessionId != null) {
            cookiesSessionManager.removeUserFromSession(sessionId);
        }
        return Response.seeOther(URI.create("/login"))
                .cookie(new NewCookie(CookiesSessionManager.COOKIE_SESSION, "", "/login", null, "Session expired", 0, false))
                .build();
    }
}
