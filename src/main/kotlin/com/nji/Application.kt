package com.nji

import com.nji.conf.BotConf
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties

@SpringBootApplication
class Application


fun main(args: Array<String>){
    SpringApplication.run(Application::class.java, *args)
}