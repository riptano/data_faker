package com.doanduyhai.data_faker

import java.io._
import java.time.format.DateTimeFormatter
import java.time.{ZoneId, ZonedDateTime}
import java.util.concurrent.TimeUnit
import java.util.{Date, Locale, UUID}

import akka.actor.{Actor, ActorLogging, ActorRef}
import com.github.javafaker.Faker

case class GenerateData(numberOfUsers: Int, minTransactions: Int, maxTransactions: Int, shouldMerge: Boolean)

class DataFakerActor(val controller: ActorRef, val actorId: String) extends Actor with ActorLogging {

  val locales = List(
    ("ca", "CAD", "CA"),
    ("de", "EUR", "DE"),
    ("en_GB", "GBP", "UK"),
    ("en_US", "USD", "US"),
    ("es", "EUR", "ES"),
    ("fr", "EUR", "FR"),
    ("ko", "KRW", "KR"),
    ("it", "EUR", "IT"),
    ("ja", "JPY", "JP"),
    ("nl", "EUR", "NL"),
    ("pl", "PLN", "PL"),
    ("pt_BR", "BRL", "BR"),
    ("ru", "RUB", "RU"),
    ("zh_CN", "CNY", "CN"))

  val fakerList = locales.map(_._1).map(locale => new Faker(new Locale(locale)))

  val paymentMode = List("Credit Card", "Cash", "Check", "Account Transfer")
  val sex = List("Male", "Female", "Gay", "Lesbian", "Trans")


  //  val usersFile = new PrintWriter(new File(s"users_$actorId.csv"))
  val purchaseFile = new PrintWriter(new File(s"${OutputDirectoryUtils.createOutputDirectory()}/purchases_$actorId.csv"))

  private def formatList(workPhone: String, homePhone: String): String =
//    s"""[\\"$workPhone\\", \\"$homePhone\\"]"""
    s"""[\"$workPhone\", \"$homePhone\"]"""

  def receive: Receive = {
    case GenerateData(usersCount, minTransactions, maxTransactions, shouldMerge) => {
      val dateFormatter = DateTimeFormatter.ISO_ZONED_DATE_TIME
      var userBuilder = new StringBuilder()

      try {

        (1 to usersCount).foreach(step => {

          val random = getRandomInt(0, 13)
          val faker = fakerList(random)
          val dateOfCreation = dateFormatter.format(DateFormat.toZonedDateTime(faker.date().past(3 * 365, TimeUnit.DAYS)))
          val currency = locales(random)._2
          val countryCode = locales(random)._3
          val userId = UUID.randomUUID
          val fn = faker.name.firstName
          val ln = faker.name.lastName

          val age = getRandomInt(18, 69)
          val sex = faker.demographic().sex()
          val domainName = faker.internet.domainName
          val homePhone = faker.phoneNumber.phoneNumber
          val workPhone = faker.phoneNumber.cellPhone
          val street = faker.address.streetAddress.replaceAll("""\|""", "")
          val city = faker.address.cityName.replaceAll("""\|""", "")
          val state = faker.address.state
          val companyName = faker.company.name.replaceAll("""\|""", "")
          val jobType = faker.company.profession

          userBuilder.append(s"""$userId|$dateOfCreation|$fn|$ln|$age|$sex|$fn.$ln@$domainName|${formatList(workPhone, homePhone)}|$street|$city|$state|$companyName|$jobType|$countryCode\n""")

          if (step % 10000 == 0) {
            //don't need to save users
            //flushData(usersFile, userBuilder, "user")
            userBuilder = new StringBuilder()
          }

          val transactionCount = getRandomInt(minTransactions, maxTransactions)
          val transactionBuilder = new StringBuilder()

          (1 to transactionCount).foreach(_ => {
            val date = dateFormatter.format(DateFormat.toZonedDateTime(faker.date().past(3 * 365, TimeUnit.DAYS)))
            val price = faker.commerce.price.toDouble
            val quantity = getRandomInt(1, 10)
            val total = price * quantity
            val item = faker.commerce.productName.replaceAll("""\|""", "")
            val payment = paymentMode(getRandomInt(0, 3))
            transactionBuilder.append(s"""$userId|$date|$item|$price|$quantity|$total|$currency|$payment|${formatList(workPhone, homePhone)}\n""")
          })
          flushData(purchaseFile, transactionBuilder, "transaction")
        })
        //don't need to save users
        //flushData(usersFile, userBuilder, "user")
        purchaseFile.flush()
        //        usersFile.flush()
      } finally {
        purchaseFile.close()
        //        usersFile.close()
        controller ! Done(actorId, shouldMerge)
      }
    }

    case unknown@_ => log.error(s"DataFakerActor receiving unknown message $unknown")
  }

  def getRandomInt(min: Int, max: Int): Int = {
    (Math.floor(Math.random() * (max - min + 1)) + min).toInt
  }

  def flushData(pw: PrintWriter, buffer: StringBuilder, file: String) {
    try {
      log.debug(s"Flushing for $file of actor $actorId")
      pw.write(buffer.toString())
    } catch {
      case e: Throwable => println(s"Exception ${e.getMessage} caught")
    }

  }
}

object DateFormat {

  def toZonedDateTime(date: Date): ZonedDateTime = {
    ZonedDateTime.ofInstant(date.toInstant, ZoneId.of("UTC"))
  }
}