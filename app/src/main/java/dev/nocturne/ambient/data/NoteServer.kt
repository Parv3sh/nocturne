package dev.nocturne.ambient.data

import fi.iki.elonen.NanoHTTPD
import java.io.File
import java.net.Inet4Address
import java.net.NetworkInterface

/**
 * Tiny LAN-only server that serves the phone doodle page and receives notes.
 * Open http://<tv-ip>:8080 on any device on the same WiFi.
 */
class NoteServer(
    private val noteFile: File,
    private val onNote: (ByteArray?) -> Unit
) : NanoHTTPD(PORT) {

    fun startServer() = start(SOCKET_READ_TIMEOUT, false)

    override fun serve(session: IHTTPSession): Response {
        return try {
            when {
                session.uri == "/" || session.uri == "/index.html" ->
                    newFixedLengthResponse(Response.Status.OK, "text/html; charset=utf-8", PAGE)
                        .apply { addHeader("Cache-Control", "no-store") }
                session.uri == "/note" && session.method == Method.POST -> upload(session)
                session.uri == "/note" && noteFile.exists() ->
                    newChunkedResponse(Response.Status.OK, "image/png", noteFile.inputStream())
                        .apply { addHeader("Cache-Control", "no-store") }
                session.uri == "/clear" -> {
                    noteFile.delete()
                    onNote(null)
                    text("cleared")
                }
                else -> newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "not found")
            }
        } catch (_: Exception) {
            newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "error")
        }
    }

    private fun upload(session: IHTTPSession): Response {
        val length = session.headers["content-length"]?.toIntOrNull() ?: 0
        if (length <= 0 || length > 8 * 1024 * 1024) return text("bad size", Response.Status.BAD_REQUEST)
        val buffer = ByteArray(length)
        var offset = 0
        while (offset < length) {
            val read = session.inputStream.read(buffer, offset, length - offset)
            if (read < 0) break
            offset += read
        }
        if (offset == 0) return text("empty", Response.Status.BAD_REQUEST)
        if (offset != length) return text("incomplete", Response.Status.BAD_REQUEST)
        val bytes = if (offset == length) buffer else buffer.copyOf(offset)
        if (!isPng(bytes)) return text("not a png", Response.Status.BAD_REQUEST)
        val temp = File(noteFile.parentFile, "note.tmp")
        temp.writeBytes(bytes)
        if (!temp.renameTo(noteFile)) {
            noteFile.writeBytes(bytes)
            temp.delete()
        }
        onNote(bytes)
        return text("ok")
    }

    private fun isPng(bytes: ByteArray): Boolean {
        if (bytes.size < 8) return false
        val signature = byteArrayOf(
            0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
        )
        return signature.indices.all { bytes[it] == signature[it] }
    }

    private fun text(body: String, status: Response.Status = Response.Status.OK): Response =
        newFixedLengthResponse(status, "text/plain", body)

    companion object {
        const val PORT = 8080

        fun localIp(): String? = runCatching {
            val all = NetworkInterface.getNetworkInterfaces().toList()
            val usable = all.filter { iface ->
                iface.isUp && !iface.isLoopback &&
                    !iface.name.startsWith("tun", ignoreCase = true) &&
                    !iface.name.startsWith("ppp", ignoreCase = true) &&
                    !iface.name.startsWith("rmnet", ignoreCase = true) &&
                    !iface.name.startsWith("dummy", ignoreCase = true)
            }
            val preferred = usable.filter {
                it.name.startsWith("wlan", ignoreCase = true) ||
                    it.name.startsWith("eth", ignoreCase = true)
            }
            (preferred + usable)
                .flatMap { it.inetAddresses.toList() }
                .filterIsInstance<Inet4Address>()
                .firstOrNull { !it.isLoopbackAddress && it.isSiteLocalAddress }
                ?.hostAddress
        }.getOrNull()

        private const val PAGE = """<!doctype html>
<html lang="en"><head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no,viewport-fit=cover">
<meta name="theme-color" content="#f4f0e5">
<title>Nocturne note</title>
<style>
html,body{margin:0;height:100%;background:#f4f0e5;color:#1d1b17;font-family:-apple-system,system-ui,sans-serif;overflow:hidden}
#wrap{position:fixed;inset:0;display:flex;flex-direction:column;min-width:0}
#bar{display:flex;gap:6px;align-items:center;padding:calc(8px + env(safe-area-inset-top)) 10px 8px;background:#ebe5d7;border-bottom:1px solid #d2cab7;flex-wrap:wrap;flex:none}
button{background:#faf7ee;color:#1d1b17;border:1px solid #c8bfab;border-radius:8px;padding:7px 10px;font-size:13px;line-height:1.1;font-family:inherit}
button:active{transform:translateY(1px)}
button.primary{background:#1d1b17;color:#f4f0e5;border-color:#1d1b17;font-weight:600}
button.danger{color:#9c3b34;border-color:#d9b8b3}
.sw{width:24px;height:24px;border-radius:50%;border:2px solid #c8bfab;padding:0}
.sw.on{box-shadow:0 0 0 2px #ebe5d7,0 0 0 4px #1d1b17}
.sz{min-width:30px;font-weight:600}
.sz.on{background:#1d1b17;color:#f4f0e5;border-color:#1d1b17}
#canvas{flex:1;min-height:0;touch-action:none;background:#f4f0e5;display:block;width:100%;height:100%}
#foot{padding:8px 10px calc(12px + env(safe-area-inset-bottom));background:#ebe5d7;border-top:1px solid #d2cab7;flex:none}
#status{font-size:12px;color:#6d685c;margin-bottom:7px;min-height:15px}
#actions{display:flex;gap:8px}
#send{flex:1;padding:14px 11px;font-size:15px}
#remove{padding:14px 12px}
</style></head>
<body><div id="wrap">
<div id="bar">
<button id="undo">Undo</button>
<button class="sz" data-s="4">S</button><button class="sz on" data-s="7">M</button><button class="sz" data-s="11">L</button>
<button class="sw on" data-c="#1d1b17" style="background:#1d1b17"></button>
<button class="sw" data-c="#8a5a2b" style="background:#8a5a2b"></button>
<button class="sw" data-c="#33566e" style="background:#33566e"></button>
<button class="sw" data-c="#a3453f" style="background:#a3453f"></button>
<button id="clear">Clear</button>
</div>
<canvas id="canvas"></canvas>
<div id="foot">
<div id="status">Draw with your finger, then send it to the TV</div>
<div id="actions">
<button id="send" class="primary">Send to TV</button>
<button id="remove" class="danger">Remove from TV</button>
</div>
</div>
</div>
<script>
var c=document.getElementById('canvas'),ctx=c.getContext('2d'),dpr=window.devicePixelRatio||1;
var drawing=false,color='#1d1b17',width=7,undo=[],touched=false;
var st=document.getElementById('status');
function fit(){var r=c.getBoundingClientRect(),w=Math.round(r.width*dpr),h=Math.round(r.height*dpr);
if(!w||!h||(c.width===w&&c.height===h))return;
var old=null;if(c.width&&c.height){old=document.createElement('canvas');old.width=c.width;old.height=c.height;old.getContext('2d').drawImage(c,0,0)}
c.width=w;c.height=h;ctx.lineCap='round';ctx.lineJoin='round';if(old)ctx.drawImage(old,0,0,w,h)}
new ResizeObserver(fit).observe(c);fit();
function snap(){try{if(undo.length>5)undo.shift();undo.push(ctx.getImageData(0,0,c.width,c.height))}catch(e){}}
function pos(e){var r=c.getBoundingClientRect();return{x:(e.clientX-r.left)*dpr,y:(e.clientY-r.top)*dpr}}
c.addEventListener('pointerdown',function(e){e.preventDefault();touched=true;snap();drawing=true;c.setPointerCapture(e.pointerId);var p=pos(e);ctx.strokeStyle=color;ctx.lineWidth=width*dpr;ctx.beginPath();ctx.moveTo(p.x,p.y)});
c.addEventListener('pointermove',function(e){if(!drawing)return;e.preventDefault();var p=pos(e);ctx.lineTo(p.x,p.y);ctx.stroke()});
c.addEventListener('pointerup',function(){drawing=false});
c.addEventListener('pointercancel',function(){drawing=false});
document.getElementById('undo').onclick=function(){var s=undo.pop();if(s)ctx.putImageData(s,0,0)};
document.getElementById('clear').onclick=function(){snap();ctx.clearRect(0,0,c.width,c.height)};
document.querySelectorAll('.sw').forEach(function(b){b.onclick=function(){document.querySelectorAll('.sw').forEach(function(x){x.classList.remove('on')});b.classList.add('on');color=b.dataset.c}});
document.querySelectorAll('.sz').forEach(function(b){b.onclick=function(){document.querySelectorAll('.sz').forEach(function(x){x.classList.remove('on')});b.classList.add('on');width=parseInt(b.dataset.s)}});
document.getElementById('send').onclick=function(){if(isEmpty()){fetch('/clear',{method:'POST'}).then(function(r){st.textContent=r.ok?'Removed from TV':'Failed'}).catch(function(){st.textContent='Failed'});return}st.textContent='Sending...';c.toBlob(function(blob){fetch('/note',{method:'POST',headers:{'Content-Type':'image/png'},body:blob}).then(function(r){st.textContent=r.ok?'Sent to TV':'Failed'}).catch(function(){st.textContent='Failed'})},'image/png')};
function isEmpty(){try{var d=ctx.getImageData(0,0,c.width,c.height).data;for(var i=3;i<d.length;i+=4){if(d[i])return false}return true}catch(e){return false}}
document.getElementById('remove').onclick=function(){fetch('/clear',{method:'POST'}).then(function(r){st.textContent=r.ok?'Removed from TV':'Failed'}).catch(function(){st.textContent='Failed'})};
fetch('/note',{cache:'no-store'}).then(function(r){if(!r.ok)throw 0;return r.blob()}).then(function(b){var img=new Image();img.onload=function(){if(touched)return;var s=Math.min(c.width/img.width,c.height/img.height),w=img.width*s,h=img.height*s;ctx.drawImage(img,(c.width-w)/2,(c.height-h)/2,w,h);st.textContent='Loaded current note'};img.src=URL.createObjectURL(b)}).catch(function(){});
</script></body></html>"""
    }
}
