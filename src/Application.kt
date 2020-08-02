package com.khassar

import com.fasterxml.jackson.databind.SerializationFeature
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.client.HttpClient
import io.ktor.client.call.receive
import io.ktor.client.engine.jetty.Jetty
import io.ktor.client.features.json.GsonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.HttpStatement
import io.ktor.client.statement.request
import io.ktor.features.ContentNegotiation
import io.ktor.jackson.jackson
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.util.toByteArray
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.core.toByteArray
import kotlinx.coroutines.runBlocking
import java.io.File
import java.util.*

/***
 使用ktor下载markdown格式的语雀文档,并且替换图片地址.使用base64码显示图片,解决防盗链的问题
 使用方法:克隆项目,修改saveFile,xAuthToken和repo,运行main方法,访问http://localhost:8080/
 *
 */

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {
    install(ContentNegotiation) {
        jackson {
            enable(SerializationFeature.INDENT_OUTPUT)
        }
    }
    //Jetty引擎
    val client = HttpClient(Jetty) {
        install(JsonFeature) {
            //启用自动json转换
            serializer = GsonSerializer()
        }
    }
    runBlocking<Unit> {
        // Sample for making a HTTP Client request
        /*
        val message = client.post<JsonSampleClass> {
            url("http://127.0.0.1:8080/path/to/endpoint")
            contentType(ContentType.Application.Json)
            body = JsonSampleClass(hello = "world")
        }
        */

    }

    routing {
        //ktor 监听/路径,get请求
        get("/") {
            //需要修改
            val saveFile = "保存目录"
            val xAuthToken="语雀个人token"
            val repo="个人路径/知识库名称"
            File(saveFile).mkdir()
            /*File(file + "\\00.目录页").mkdir()*/
            //<YuQueDirectoryStructureList>:返回的json对象,上面启用了自动json转换
            val yuQueDirectoryStructureList =
                client.get<YuQueDirectoryStructureList>("https://www.yuque.com/api/v2/repos/${repo}/toc") {
                    //个人Token
                    header("X-Auth-Token", xAuthToken)
                    //应用名称
                    header("User-Agent", "ktor-markdown-yuque")
                }
            var lastDepth = 1
            var currentFile: String = saveFile
            for (item in yuQueDirectoryStructureList.data) {
                //通过判断当前depth和上一次depth来得出文件或文件夹应该放在哪个目录
                if (item.depth > lastDepth) {
                    lastDepth = item.depth
                } else if (item.depth < lastDepth) {
                    for (i in 0 until lastDepth - item.depth) {
                        currentFile = currentFile.substring(0, currentFile.lastIndexOf("\\"))
                    }
                    lastDepth = item.depth
                }
                if (item.doc_id == "") {
                    //doc_id等于空说明是文件夹
                    if (File(currentFile).listFiles() != null) {
                        //文件夹命名规则为00.xxx
                        currentFile =
                            currentFile + "\\" + MyUtils.getFileSerialNumberString(File(currentFile).listFiles().size) + item.title.replace(
                                " ",
                                ""
                            )
                        File(currentFile).mkdir()
                    } else {
                        //创建第一个文件夹
                        currentFile = currentFile + "\\" + "00." + item.title.replace(" ", "")
                        File(currentFile).mkdir()
                    }

                } else {
                    //oc_id不等于空,保存doc内容并替换图片地址为base64码
                    val temp: String = File(currentFile).path
                    if (File(currentFile).listFiles() != null) {
                        //文件命名规则为00.xxx.md
                        currentFile =
                            currentFile + "\\" + MyUtils.getFileSerialNumberString(File(currentFile).listFiles().size) + item.title.replace(
                                " ",
                                ""
                            ) + ".md"
                        File(currentFile).createNewFile()

                    } else {
                        //创建第一个文件
                        currentFile = currentFile + "\\00." + item.title.replace(" ", "") + ".md"
                        File(currentFile).createNewFile()
                    }
                    //获取具体doc的草稿内容,不需要发布即可获得文档内容
                    val docDetail =
                        client.get<DocDetail>("https://www.yuque.com/api/v2/repos/${repo}/docs/${item.doc_id}?raw=0") {
                            //个人Token
                            header("X-Auth-Token", xAuthToken)
                            //应用名称
                            header("User-Agent", "ktor-markdown-yuque")
                        }
                    //深拷贝草稿内容
                    var md = String(docDetail.data.body_draft.toByteArray())
                    var url = String(md.toByteArray())
                    while (md.contains("https://cdn.nlark.com")) {
                        //https://cdn.nlark.com是语雀图片的地址,由于不会正则表达式,这里简单写了
                        url = url.substring(url.indexOf("https://cdn.nlark.com"), url.length - 1)
                        url = url.substring(0, url.indexOf(")"))
                        //切割出的图片网址通过ktor请求,由于是异步请求,先将url传进去
                        client.get<HttpStatement>(url).execute { response: HttpResponse ->
                            // Response is not downloaded here.
                            val channel = response.receive<ByteReadChannel>()
                            //获取通道转换成ByteArray再转换成base64码,写入文档内容
                            md += "\n[img${Base64.getEncoder().encodeToString(response.request.url.toString().toByteArray())}]:data:image/png;base64,${Base64.getEncoder().encodeToString(channel.toByteArray())}\n"
                        }
                        /*将文档内容里的图片地址换成以下格式img377我这里放的是字符串的base64码
                        * ![alt][img377]
                        * [img377]:data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAcagoiUFY.....
                        * Markdown里按照上面格式就可以显示base64图片,但是编辑起来可能会卡,不过我完全是在语雀上编辑,所以不存在这个问题
                        * */
                        md = md.replace("(" + url + ")", "[img${Base64.getEncoder().encodeToString(url.toByteArray())}]")
                        url = String(md.toByteArray())


                    }
                    //将修改后的文档内容写入文件
                    File(currentFile).writeText(md)
                    //将文件路径还原成文件夹路径
                    currentFile = temp
                }
            }

            //ktor返回json
            call.respond(yuQueDirectoryStructureList)
        }

    }
}


