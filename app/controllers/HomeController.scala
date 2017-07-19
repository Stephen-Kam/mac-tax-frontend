package controllers

import java.io.{File, PrintWriter}
import javax.inject.Inject

import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.MessagesApi
import play.api.libs.json.{JsValue, Json, OFormat}
import play.api.libs.ws._
import play.api.mvc._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * This controller creates an `Action` to handle HTTP requests to the
  * application's home page.
  */

class HomeController @Inject()(ws: WSClient, val messagesApi: MessagesApi) extends Controller with play.api.i18n.I18nSupport {

  val appleForm = Form(single(
    "appleCount" -> number
  ))

  val mackintoshForm = Form(single(
    "mackCount" -> number
  ))

  /**
    * Create an Action to render an HTML page.
    *
    * The configuration in the `routes` file means that this method
    * will be called when the application receives a `GET` request with
    * a path of `/`.
    */
  def index() = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.appleInput(appleForm))
  }

  def submitApple() = Action { implicit request: Request[AnyContent] =>
    appleForm.bindFromRequest().fold(
      formWithErrors => BadRequest(views.html.appleInput(formWithErrors)),
      validForm => Redirect(routes.HomeController.mackintoshInput()).withSession("appleCount" -> s"$validForm")
    )
  }

  def submitMackintosh() = Action.async { implicit request: Request[AnyContent] =>
    mackintoshForm.bindFromRequest().fold(
      formWithErrors => Future.successful(BadRequest(views.html.mackintoshInput(formWithErrors))),
      validForm =>
        submitBackend(request.session, validForm).map {
          _ => Redirect(routes.HomeController.results())
        }
    )
  }

  def mackintoshInput() = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.mackintoshInput(mackintoshForm))
  }

  def results() = Action { implicit request: Request[AnyContent] =>
    val jsonResult = readJson()
    val result = Json.fromJson[CalculatorResult](jsonResult).get
    Ok(views.html.results(result))
  }

  def readJson(): JsValue = {
    Json.parse(getClass.getResourceAsStream("/public/result.json"))
  }

  def submitBackend(session: Session, mackCount: Int): Future[Unit] = {
    ws.url("http://localhost:9001/calculate").withQueryString(
      "apps" -> session.get("appleCount").get,
      "macs" -> s"$mackCount").get().map {
      result =>
        val body = result.body
        val p = new PrintWriter(new File("public/result.json"))
        p.write(body)
        p.close()
    }
  }
}

case class CalculatorResult(apps: Int, macs: Int,
                            costPerMac: Double, costPerApp: Double,
                            totalMac: Double, totalApp: Double,
                            total: Double) {


}

object CalculatorResult {
  implicit val jsonFormatter: OFormat[CalculatorResult] = Json.format[CalculatorResult]
}