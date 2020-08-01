package com.khassar


data class YuQueDirectoryStructureList(
    val data:List<YuQueDirectory>
)
data class YuQueDirectory(
    var type:String="",
    var title:String="",
    var uuid:String="",
    var parent_uuid:String="",
    var doc_id:String="",
    var level:String="",
    var id:String="",
    var open_window:String="",
    var visible:String="",
    var child_uuid:String="",
    var sibling_uuid:String="",
    var depth:Int=-1,
    var slug:String="",
    var prev_uuid:String=""

)

data class DocDetail(
    var data:DocDetailSerializer,
    var abilities:abilities
)
data class abilities(
    var update:Boolean,
    var destroy:Boolean
)
data class DocDetailSerializer(
    var id:String="",
    var slug:String="",
    var title:String="",
    var book_id:String="",
    //var book:String="",
    var user_id:String="",
    //var user:String="",
    var format:String="",
    var body:String="",
    var body_draft:String="",
    var body_html:String="",
    var body_lake:String="",
    var creator_id:String="",
    var public:String="",
    var status:String="",
    var likes_count:String="",
    var comments_count:String="",
    var content_updated_at:String="",
    var deleted_at:String="",
    var created_at:String="",
    var updated_at:String=""
)
